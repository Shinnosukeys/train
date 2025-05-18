package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.DailyTrain;
import com.chovysun.train.business.domain.DailyTrainTicket;
import com.chovysun.train.business.domain.TrainStation;
import com.chovysun.train.business.enums.SeatTypeEnum;
import com.chovysun.train.business.mapper.DailyTrainTicketMapper;
import com.chovysun.train.business.req.DailyTrainTicketQueryReq;
import com.chovysun.train.business.req.DailyTrainTicketSaveReq;
import com.chovysun.train.business.resp.DailyTrainTicketQueryResp;
import com.chovysun.train.business.service.IDailyTrainTicketService;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chovysun.train.business.enums.TrainTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class DailyTrainTicketServiceImpl extends ServiceImpl<DailyTrainTicketMapper, DailyTrainTicket> implements IDailyTrainTicketService {

    @Resource
    private DailyTrainTicketMapper dailyTrainTicketMapper;

    @Resource
    private TrainStationServiceImpl trainStationServiceImpl;

    @Resource
    private DailyTrainSeatServiceImpl dailyTrainSeatServiceImpl;

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketServiceImpl.class);
    
    @Override
    public void save(DailyTrainTicketSaveReq req) {
        DateTime dataTime = DateTime.now();
        DailyTrainTicket DailyTrainTicket = BeanUtil.copyProperties(req, DailyTrainTicket.class);
        if (ObjectUtil.isNull(DailyTrainTicket.getId())) {
            DailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
            DailyTrainTicket.setCreateTime(dataTime);
            DailyTrainTicket.setUpdateTime(dataTime);
            dailyTrainTicketMapper.insert(DailyTrainTicket);
        } else {
            DailyTrainTicket.setUpdateTime(dataTime);
            dailyTrainTicketMapper.updateById(DailyTrainTicket);
        }
    }

    @Override
    public PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<DailyTrainTicket> queryWrapper = new QueryWrapper<>();

        // 添加排序条件
        // "`date` desc, start_time asc, train_code asc, `start_index` asc, `end_index` asc");
        queryWrapper.orderByDesc("date").orderByAsc("start_time", "train_code", "start_index", "end_index");

        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotNull(req.getDate())) {
            queryWrapper.eq("date", req.getDate());
        }
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }
        if (ObjectUtil.isNotEmpty(req.getStart())) {
            queryWrapper.eq("start", req.getStart());
        }
        if (ObjectUtil.isNotEmpty(req.getEnd())) {
            queryWrapper.eq("end", req.getEnd());
        }

        // 分页查询
        Page<DailyTrainTicket> page = new Page<>(req.getPage(), req.getSize());
        IPage<DailyTrainTicket> ticketPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", ticketPage.getTotal());
        LOG.info("总页数：{}", ticketPage.getPages());

        // 转换为响应对象
        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(ticketPage.getRecords(), DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(ticketPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Transactional
    public void genDailyTrainTicket(DailyTrain dailyTrain, Date date, String trainCode) {
        LOG.info("生成日期【{}】车次【{}】的余票信息开始", DateUtil.formatDate(date), trainCode);

        // 删除某日某车次的余票信息
        QueryWrapper<DailyTrainTicket> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("train_code", trainCode);
        remove(deleteWrapper);
        LOG.info("已清空 日期【{}】车次【{}】原有的余票记录", date, trainCode);

        // 查出某车次的所有的车站信息
        List<TrainStation> stationList = trainStationServiceImpl.selectByTrainCode(trainCode);
        if (CollUtil.isEmpty(stationList)) {
            LOG.info("该车次没有车站基础数据，生成该车次的余票信息结束");
            return;
        }

        DateTime now = DateTime.now();
        for (int i = 0; i < stationList.size(); i++) {
            // 得到出发站
            TrainStation trainStationStart = stationList.get(i);
            BigDecimal sumKM = BigDecimal.ZERO;
            for (int j = (i + 1); j < stationList.size(); j++) {
                TrainStation trainStationEnd = stationList.get(j);
                sumKM = sumKM.add(trainStationEnd.getKm());

                DailyTrainTicket dailyTrainTicket = new DailyTrainTicket();
                dailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(trainCode);
                dailyTrainTicket.setStart(trainStationStart.getName());
                dailyTrainTicket.setStartPinyin(trainStationStart.getNamePinyin());
                dailyTrainTicket.setStartTime(trainStationStart.getOutTime());
                dailyTrainTicket.setStartIndex(trainStationStart.getIndex());
                dailyTrainTicket.setEnd(trainStationEnd.getName());
                dailyTrainTicket.setEndPinyin(trainStationEnd.getNamePinyin());
                dailyTrainTicket.setEndTime(trainStationEnd.getInTime());
                dailyTrainTicket.setEndIndex(trainStationEnd.getIndex());

                // TODO 这块固定的价格值可以提到循环外面
                int ydz = dailyTrainSeatServiceImpl.countSeat(date, trainCode, SeatTypeEnum.YDZ.getCode());
                int edz = dailyTrainSeatServiceImpl.countSeat(date, trainCode, SeatTypeEnum.EDZ.getCode());
                int rw = dailyTrainSeatServiceImpl.countSeat(date, trainCode, SeatTypeEnum.RW.getCode());
                int yw = dailyTrainSeatServiceImpl.countSeat(date, trainCode, SeatTypeEnum.YW.getCode());

                // 票价 = 里程之和 * 座位单价 * 车次类型系数
                String trainType = dailyTrain.getType();
                // 计算票价系数：TrainTypeEnum.priceRate
                BigDecimal priceRate = EnumUtil.getFieldBy(TrainTypeEnum::getPriceRate, TrainTypeEnum::getCode, trainType);

                // 里程 × 座位单价 × 车型系数, 四舍五入保留2位小数
                BigDecimal ydzPrice = sumKM.multiply(SeatTypeEnum.YDZ.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal edzPrice = sumKM.multiply(SeatTypeEnum.EDZ.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal rwPrice = sumKM.multiply(SeatTypeEnum.RW.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ywPrice = sumKM.multiply(SeatTypeEnum.YW.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);

                dailyTrainTicket.setYdz(ydz);
                dailyTrainTicket.setYdzPrice(ydzPrice);
                dailyTrainTicket.setEdz(edz);
                dailyTrainTicket.setEdzPrice(edzPrice);
                dailyTrainTicket.setRw(rw);
                dailyTrainTicket.setRwPrice(rwPrice);
                dailyTrainTicket.setYw(yw);
                dailyTrainTicket.setYwPrice(ywPrice);
                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);
                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }
        LOG.info("生成日期【{}】车次【{}】的余票信息结束", DateUtil.formatDate(date), trainCode);
    }

    public DailyTrainTicket selectByUnique(Date date, String trainCode, String start, String end) {
        QueryWrapper<DailyTrainTicket> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", date).
                eq("train_code", trainCode).
                eq("start", start).
                eq("end", end);
        return dailyTrainTicketMapper.selectOne(queryWrapper);
    }
}
