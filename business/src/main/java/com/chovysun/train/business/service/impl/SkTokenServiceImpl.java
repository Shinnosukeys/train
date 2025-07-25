package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.SkToken;
import com.chovysun.train.business.enums.RedisKeyPreEnum;
import com.chovysun.train.business.mapper.SkTokenMapper;
import com.chovysun.train.business.req.SkTokenQueryReq;
import com.chovysun.train.business.req.SkTokenSaveReq;
import com.chovysun.train.business.resp.SkTokenQueryResp;
import com.chovysun.train.business.service.ISkTokenService;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenServiceImpl extends ServiceImpl<SkTokenMapper, SkToken> implements ISkTokenService {

    @Resource
    private SkTokenMapper skTokenMapper;

    @Resource
    private DailyTrainSeatServiceImpl dailyTrainSeatService;

    @Resource
    private DailyTrainStationServiceImpl dailyTrainStationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.profiles.active}")
    private String env;


    private static final Logger LOG = LoggerFactory.getLogger(SkTokenServiceImpl.class);

    @Override
    public void save(SkTokenSaveReq req) {
        DateTime dataTime = DateTime.now();
        SkToken SkToken = BeanUtil.copyProperties(req, SkToken.class);
        if (ObjectUtil.isNull(SkToken.getId())) {
            SkToken.setId(SnowUtil.getSnowflakeNextId());
            SkToken.setCreateTime(dataTime);
            SkToken.setUpdateTime(dataTime);
            skTokenMapper.insert(SkToken);
        } else {
            SkToken.setUpdateTime(dataTime);
            skTokenMapper.updateById(SkToken);
        }
    }

    @Override
    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<SkToken> queryWrapper = new QueryWrapper<>();

        // 添加排序条件
        queryWrapper.orderByDesc("id");

        // 分页查询
        Page<SkToken> page = new Page<>(req.getPage(), req.getSize());
        IPage<SkToken> ticketPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", ticketPage.getTotal());
        LOG.info("总页数：{}", ticketPage.getPages());

        // 转换为响应对象
        List<SkTokenQueryResp> list = BeanUtil.copyToList(ticketPage.getRecords(), SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(ticketPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    /**
     * 校验令牌
     */

    @Override
    public boolean validSkToken(Date date, String trainCode, Long memberId) {
        LOG.info("会员【{}】获取日期【{}】车次【{}】的令牌开始", memberId, DateUtil.formatDate(date), trainCode);

        // TODO 缺乏用户级别的防刷策略
        // 需要去掉这段，否则发布生产后，体验多人排队功能时，会因拿不到锁而返回：等待5秒，加入20人时，只有第1次循环能拿到锁
         if (!env.equals("dev")) {
             // 先获取令牌锁，再校验令牌余量，防止机器人抢票，lockKey就是令牌，用来表示【谁能做什么】的一个凭证
             String lockKey = RedisKeyPreEnum.SK_TOKEN + "-" + DateUtil.formatDate(date) + "-" + trainCode + "-" + memberId;
             Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
             if (Boolean.TRUE.equals(setIfAbsent)) {
                 LOG.info("恭喜，抢到令牌锁了！lockKey：{}", lockKey);
             } else {
                 LOG.info("很遗憾，没抢到令牌锁！lockKey：{}", lockKey);
                 return false;
             }
         }

        // 构建 Redis 键
        String skTokenCountKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-" + DateUtil.formatDate(date) + "-" + trainCode;

        // 1. 先从 Redis 尝试扣减令牌
        Object skTokenCount = stringRedisTemplate.opsForValue().get(skTokenCountKey);
        if (skTokenCount != null) {
            LOG.info("缓存中有该车次令牌大闸的key：{}", skTokenCountKey);
            Long count = stringRedisTemplate.opsForValue().decrement(skTokenCountKey, 1); // 原子扣减
            if (count < 0L) {
                LOG.error("获取令牌失败：{}", skTokenCountKey);
                return false;
            }
            LOG.info("获取令牌后，令牌余数：{}", count);
            stringRedisTemplate.expire(skTokenCountKey, 600, TimeUnit.SECONDS); // 延长缓存有效期

            // 每扣减 N 次同步一次数据库（示例：每 5 次）
            if (count % 5 == 0) {
                syncTokenToDatabase(date, trainCode, count); // 异步更新数据库（建议用线程池或消息队列）
            }
            return true;
        }

        // TODO 防止缓存穿透的问题
        // 2. Redis 无缓存时，查询数据库并初始化缓存
        LOG.info("缓存中没有该车次令牌大闸的key：{}，开始查询数据库", skTokenCountKey);
        try {
            // 使用 QueryWrapper 构建查询条件（利用唯一索引：date + train_code）
            QueryWrapper<SkToken> wrapper = new QueryWrapper<>();
            wrapper.eq("date", date)
                    .eq("train_code", trainCode);

            // 查询单条记录（唯一索引确保最多一条）
            SkToken skToken = skTokenMapper.selectOne(wrapper);

            if (skToken == null) {
                LOG.info("找不到日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
                return false;
            }

            if (skToken.getCount() <= 0) {
                LOG.info("日期【{}】车次【{}】的令牌余量为0", DateUtil.formatDate(date), trainCode);
                return false;
            }

            // 计算新的余量并更新缓存（不立即更新数据库，采用延迟同步）
            int newCount = skToken.getCount() - 1;
            LOG.info("将该车次令牌大闸放入缓存中，key: {}， count: {}", skTokenCountKey, newCount);
            stringRedisTemplate.opsForValue().set(skTokenCountKey, String.valueOf(newCount), 60, TimeUnit.SECONDS);

            // 记录本次操作（可异步批量更新数据库，例如通过定时任务）
            skToken.setCount(newCount);
            //skTokenMapper.updateById(skToken); // 如需强一致性，取消注释（但会增加数据库压力）
            return true;

        } catch (Exception e) {
            LOG.error("数据库查询令牌异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步同步令牌余量到数据库
     */
    private void syncTokenToDatabase(Date date, String trainCode, long cacheCount) {
        try {
            // 通过唯一索引查询数据库记录
            QueryWrapper<SkToken> wrapper = new QueryWrapper<>();
            wrapper.eq("date", date)
                    .eq("train_code", trainCode);

            SkToken skToken = skTokenMapper.selectOne(wrapper);
            if (skToken != null && skToken.getCount() != cacheCount) {
                skToken.setCount((int) cacheCount);
                skToken.setUpdateTime(DateTime.now());
                skTokenMapper.updateById(skToken); // 仅在缓存与数据库不一致时更新
            }
        } catch (Exception e) {
            LOG.error("同步令牌到数据库失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 初始化
     */
    @Override
    public void genDaily(Date date, String trainCode) {
        LOG.info("删除日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
        QueryWrapper<SkToken> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("train_code", trainCode);
        remove(deleteWrapper);

        DateTime now = DateTime.now();
        SkToken skToken = new SkToken();
        skToken.setDate(date);
        skToken.setTrainCode(trainCode);
        skToken.setId(SnowUtil.getSnowflakeNextId());
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);

        int seatCount = dailyTrainSeatService.countSeat(date, trainCode);
        LOG.info("车次【{}】座位数：{}", trainCode, seatCount);

        long stationCount = dailyTrainStationService.countByTrainCode(date, trainCode);
        LOG.info("车次【{}】到站数：{}", trainCode, stationCount);

        // 3/4需要根据实际卖票比例来定，一趟火车最多可以卖（seatCount * stationCount）张火车票
        int count = (int) (seatCount * stationCount); // * 3/4);
        LOG.info("车次【{}】初始生成令牌数：{}", trainCode, count);
        skToken.setCount(count);

        skTokenMapper.insert(skToken);
    }
}
