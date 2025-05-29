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
import com.chovysun.train.business.domain.DailyTrainStation;
import com.chovysun.train.business.domain.TrainStation;
import com.chovysun.train.business.mapper.DailyTrainStationMapper;
import com.chovysun.train.business.req.DailyTrainStationQueryReq;
import com.chovysun.train.business.req.DailyTrainStationSaveReq;
import com.chovysun.train.business.resp.DailyTrainStationQueryResp;
import com.chovysun.train.business.service.IDailyTrainStationService;
import com.chovysun.train.business.service.ITrainStationService;
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
public class DailyTrainStationServiceImpl extends ServiceImpl<DailyTrainStationMapper, DailyTrainStation> implements IDailyTrainStationService {

    @Resource
    private DailyTrainStationMapper dailyTrainStationMapper;

    @Resource
    private ITrainStationService trainStationService;

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainStationServiceImpl.class);

    @Override
    public void save(DailyTrainStationSaveReq req) {
        DateTime dataTime = DateTime.now();
        DailyTrainStation DailyTrainStation = BeanUtil.copyProperties(req, DailyTrainStation.class);
        if (ObjectUtil.isNull(DailyTrainStation.getId())) {
            DailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            DailyTrainStation.setCreateTime(dataTime);
            DailyTrainStation.setUpdateTime(dataTime);
            dailyTrainStationMapper.insert(DailyTrainStation);
        } else {
            DailyTrainStation.setUpdateTime(dataTime);
            dailyTrainStationMapper.updateById(DailyTrainStation);
        }
    }

    @Override
    public PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<DailyTrainStation> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByDesc("date").orderByAsc("train_code", "`index`");


        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getDate())) {
            queryWrapper.eq("date", req.getDate());
        }
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }

        // 分页查询
        Page<DailyTrainStation> page = new Page<>(req.getPage(), req.getSize());
        IPage<DailyTrainStation> trainPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainPage.getTotal());
        LOG.info("总页数：{}", trainPage.getPages());

        // 转换为响应对象
        List<DailyTrainStationQueryResp> list = BeanUtil.copyToList(trainPage.getRecords(), DailyTrainStationQueryResp.class);

        PageResp<DailyTrainStationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Transactional
    public void genDailyTrainStation(Date date, String trainCode) {
        LOG.info("开始生成日期【{}】车次【{}】的车站信息", date, trainCode);

        // 删除改日的座位信息
        QueryWrapper<DailyTrainStation> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("train_code", trainCode);
        remove(deleteWrapper);
        LOG.info("已清空 日期【{}】车次【{}】原有的车站记录", date, trainCode);

        // 查出某车次的所有的车站信息
        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);
        if (CollUtil.isEmpty(trainStationList)) {
            LOG.info("该车次没有座位基础数据，生成该车次的车站信息结束");
            return;
        }

        // 根据基础车站信息生成每日车站信息
        for (TrainStation trainStation : trainStationList) {
            DateTime now = DateTime.now();
            DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(trainStation, DailyTrainStation.class);
            dailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStation.setDate(date);
            dailyTrainStationMapper.insert(dailyTrainStation);
        }

        LOG.info("生成日期【{}】车次【{}】的车站信息结束", DateUtil.formatDate(date), trainCode);
    }

    /**
     * 按车次查询全部车站
     */
    public long countByTrainCode(Date date, String trainCode) {
        QueryWrapper<DailyTrainStation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", date)
                .eq("train_code", trainCode);

        long l = dailyTrainStationMapper.selectCount(queryWrapper);
        if (l == 0L) {
            return -1;
        }
        return l;
    }
}
