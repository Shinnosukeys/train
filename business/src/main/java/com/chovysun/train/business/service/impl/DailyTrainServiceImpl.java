package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.DailyTrain;
import com.chovysun.train.business.domain.Train;
import com.chovysun.train.business.mapper.DailyTrainMapper;
import com.chovysun.train.business.req.DailyTrainQueryReq;
import com.chovysun.train.business.req.DailyTrainSaveReq;
import com.chovysun.train.business.resp.DailyTrainQueryResp;
import com.chovysun.train.business.service.IDailyTrainService;
import com.chovysun.train.business.service.ITrainService;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainServiceImpl extends ServiceImpl<DailyTrainMapper, DailyTrain> implements IDailyTrainService {

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private ITrainService trainService;

    @Resource
    private DailyTrainStationServiceImpl dailyTrainStationServiceImpl;

    @Resource
    private DailyTrainCarriageServiceImpl dailyTrainCarriageServiceImpl;

    @Resource
    private DailyTrainSeatServiceImpl dailyTrainSeatServiceImpl;

    @Resource
    private DailyTrainTicketServiceImpl dailyTrainTicketServiceImpl;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);

    @Override
    public void save(DailyTrainSaveReq req) {
        DateTime dataTime = DateTime.now();
        DailyTrain dailyTrain = BeanUtil.copyProperties(req, DailyTrain.class);
        if (ObjectUtil.isNull(dailyTrain.getId())) {

            // TODO date和code的唯一性校验

            dailyTrain.setId(SnowUtil.getSnowflakeNextId());
            dailyTrain.setCreateTime(dataTime);
            dailyTrain.setUpdateTime(dataTime);
            dailyTrainMapper.insert(dailyTrain);
        } else {
            dailyTrain.setUpdateTime(dataTime);
            dailyTrainMapper.updateById(dailyTrain);
        }
    }

    @Override
    public PageResp<DailyTrainQueryResp> queryList(DailyTrainQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<DailyTrain> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByDesc("date").orderByAsc("code");


        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getDate())) {
            queryWrapper.eq("date", req.getDate());
        }
        if (ObjectUtil.isNotEmpty(req.getCode())) {
            queryWrapper.eq("code", req.getCode());
        }

        // 分页查询
        Page<DailyTrain> page = new Page<>(req.getPage(), req.getSize());
        IPage<DailyTrain> trainPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainPage.getTotal());
        LOG.info("总页数：{}", trainPage.getPages());

        // 转换为响应对象
        List<DailyTrainQueryResp> list = BeanUtil.copyToList(trainPage.getRecords(), DailyTrainQueryResp.class);

        PageResp<DailyTrainQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    @Transactional
    public void genDaily(Date date) {
        List<Train> trainList = trainService.selectAll();
        if (CollUtil.isEmpty(trainList)) {
            LOG.info("没有车次基础数据，任务结束");
        }
        for (Train train : trainList) {
            genDailyTrain(date, train);
        }
    }


    public void genDailyTrain(Date date, Train train) {
        LOG.info("开始生成日期【{}】车次【{}】的信息", date, train.getCode());

        // 删除改日的座位信息
        QueryWrapper<DailyTrain> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("code", train.getCode());
        remove(deleteWrapper);
        LOG.info("已清空 日期【{}】车次【{}】原有的记录", date, train.getCode());

        DateTime now = DateTime.now();
        DailyTrain dailyTrain = BeanUtil.copyProperties(train, DailyTrain.class);
        dailyTrain.setId(SnowUtil.getSnowflakeNextId());
        dailyTrain.setCreateTime(now);
        dailyTrain.setUpdateTime(now);
        dailyTrain.setDate(date);
        dailyTrainMapper.insert(dailyTrain);

        // 生成该车次的车站数据
        dailyTrainStationServiceImpl.genDailyTrainStation(date, train.getCode());

        // 生成该车次的车厢数据
        dailyTrainCarriageServiceImpl.genDailyTrainCarriage(date, train.getCode());

        // 生成该车次的座位数据
        dailyTrainSeatServiceImpl.genDailyTrainSeat(date, train.getCode());

        // 生成该车次的余票数据
        dailyTrainTicketServiceImpl.genDailyTrainTicket(dailyTrain, date, train.getCode());

        LOG.info("生成日期【{}】车次【{}】的信息结束", DateUtil.formatDate(date), train.getCode());
    }
}
