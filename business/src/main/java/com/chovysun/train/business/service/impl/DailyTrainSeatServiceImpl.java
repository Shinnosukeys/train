package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.DailyTrainSeat;
import com.chovysun.train.business.domain.TrainSeat;
import com.chovysun.train.business.domain.TrainStation;
import com.chovysun.train.business.mapper.DailyTrainSeatMapper;
import com.chovysun.train.business.req.DailyTrainSeatQueryReq;
import com.chovysun.train.business.req.DailyTrainSeatSaveReq;
import com.chovysun.train.business.resp.DailyTrainSeatQueryResp;
import com.chovysun.train.business.service.IDailyTrainSeatService;
import com.chovysun.train.business.service.ITrainSeatService;
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
public class DailyTrainSeatServiceImpl extends ServiceImpl<DailyTrainSeatMapper, DailyTrainSeat> implements IDailyTrainSeatService {

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private ITrainSeatService trainSeatService;

    @Resource
    private ITrainStationService trainStationService;

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatServiceImpl.class);

    @Override
    public void save(DailyTrainSeatSaveReq req) {
        DateTime dataTime = DateTime.now();
        DailyTrainSeat DailyTrainSeat = BeanUtil.copyProperties(req, DailyTrainSeat.class);
        if (ObjectUtil.isNull(DailyTrainSeat.getId())) {
            DailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            DailyTrainSeat.setCreateTime(dataTime);
            DailyTrainSeat.setUpdateTime(dataTime);
            dailyTrainSeatMapper.insert(DailyTrainSeat);
        } else {
            DailyTrainSeat.setUpdateTime(dataTime);
            dailyTrainSeatMapper.updateById(DailyTrainSeat);
        }
    }

    @Override
    public PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<DailyTrainSeat> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByDesc("date").orderByAsc("train_code", "carriage_index");


        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }

        // 分页查询
        Page<DailyTrainSeat> page = new Page<>(req.getPage(), req.getSize());
        IPage<DailyTrainSeat> trainPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainPage.getTotal());
        LOG.info("总页数：{}", trainPage.getPages());

        // 转换为响应对象
        List<DailyTrainSeatQueryResp> list = BeanUtil.copyToList(trainPage.getRecords(), DailyTrainSeatQueryResp.class);

        PageResp<DailyTrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Transactional
    public void genDailyTrainSeat(Date date, String trainCode) {
        LOG.info("开始生成日期【{}】车次【{}】的座位信息", date, trainCode);

        // 删除改日的座位信息
        QueryWrapper<DailyTrainSeat> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("train_code", trainCode);
        remove(deleteWrapper);
        LOG.info("已清空 日期【{}】车次【{}】原有的座位记录", date, trainCode);

        List<TrainStation> stationList = trainStationService.selectByTrainCode(trainCode);
        String sell = StrUtil.fillBefore("", '0', stationList.size() - 1);

        // 查出某车次的所有的座位信息
        List<TrainSeat> trainSeatList = trainSeatService.selectByTrainCode(trainCode);
        if (CollUtil.isEmpty(trainSeatList)) {
            LOG.info("该车次没有座位基础数据，生成该车次的座位信息结束");
            return;
        }

        // 根据基础座位信息生成每日座位信息
        for (TrainSeat trainSeat : trainSeatList) {
            DateTime now = DateTime.now();
            DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(trainSeat, DailyTrainSeat.class);
            dailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeat.setDate(date);
            dailyTrainSeat.setSell(sell);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        }

        LOG.info("生成日期【{}】车次【{}】的座位信息结束", DateUtil.formatDate(date), trainCode);
    }

    public int countSeat(Date date, String trainCode) {
        return countSeat(date, trainCode, null);
    }

    public int countSeat(Date date, String trainCode, String seatType) {
        QueryWrapper<DailyTrainSeat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", date)
                .eq("train_code", trainCode);

        if (seatType != null) {
            queryWrapper.eq("seat_type", seatType);
        }

        long l = dailyTrainSeatMapper.selectCount(queryWrapper);
        if (l == 0L) {
            return -1;
        }
        return (int) l;
    }


    public List<DailyTrainSeat> selectByCarriage(Date date, String trainCode, Integer carriageIndex) {
        QueryWrapper<DailyTrainSeat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", date).
                eq("train_code", trainCode).
                eq("carriage_index", carriageIndex).
                orderByAsc("carriage_seat_index");
        return dailyTrainSeatMapper.selectList(queryWrapper);
    }
}
