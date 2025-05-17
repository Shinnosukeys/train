package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.TrainCarriage;
import com.chovysun.train.business.domain.TrainSeat;
import com.chovysun.train.business.enums.SeatColEnum;
import com.chovysun.train.business.mapper.TrainSeatMapper;
import com.chovysun.train.business.req.TrainSeatQueryReq;
import com.chovysun.train.business.req.TrainSeatSaveReq;
import com.chovysun.train.business.resp.TrainSeatQueryResp;
import com.chovysun.train.business.service.ITrainCarriageService;
import com.chovysun.train.business.service.ITrainSeatService;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainSeatServiceImpl extends ServiceImpl<TrainSeatMapper, TrainSeat> implements ITrainSeatService {

    @Resource
    private TrainSeatMapper TrainSeatMapper;

    @Resource
    private ITrainCarriageService trainCarriageService;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);

    @Override
    public void save(TrainSeatSaveReq req) {
        DateTime dataTime = DateTime.now();
        TrainSeat TrainSeat = BeanUtil.copyProperties(req, TrainSeat.class);

        if (ObjectUtil.isNull(TrainSeat.getId())) {
            TrainSeat.setId(SnowUtil.getSnowflakeNextId());
            TrainSeat.setCreateTime(dataTime);
            TrainSeat.setUpdateTime(dataTime);
            TrainSeatMapper.insert(TrainSeat);
        } else {
            TrainSeat.setUpdateTime(dataTime);
            TrainSeatMapper.updateById(TrainSeat);
        }
    }

    @Override
    public PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<TrainSeat> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByAsc("train_code", "carriage_index", "carriage_seat_index"); // `index` 加反引号避免关键字问题

        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }

        // 分页查询
        Page<TrainSeat> page = new Page<>(req.getPage(), req.getSize());
        IPage<TrainSeat> TrainSeatPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", TrainSeatPage.getTotal());
        LOG.info("总页数：{}", TrainSeatPage.getPages());

        // 转换为响应对象
        List<TrainSeatQueryResp> list = BeanUtil.copyToList(TrainSeatPage.getRecords(), TrainSeatQueryResp.class);

        PageResp<TrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(TrainSeatPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<TrainSeat> selectByTrainCode(String trainCode) {
        QueryWrapper<TrainSeat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("train_code", trainCode).orderByAsc("`index`");;
        return TrainSeatMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void genTrainSeat(String trainCode) {
        DateTime now = DateTime.now();
        LOG.info("开始生成车次 [{}] 的座位信息", trainCode);

        // 1. 清空当前车次下的所有座位记录
        QueryWrapper<TrainSeat> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("train_code", trainCode);
        remove(deleteWrapper);
        LOG.info("已清空车次 [{}] 原有的座位记录", trainCode);

        // 2. 获取当前车次下的所有车厢
        List<TrainCarriage> carriageList = trainCarriageService.selectByTrainCode(trainCode);
        if (carriageList.isEmpty()) {
            LOG.warn("车次 [{}] 下无车厢信息，无法生成座位", trainCode);
            return;
        }
        LOG.info("车次 [{}] 共有 {} 节车厢", trainCode, carriageList.size());

        // 3. 循环生成每节车厢的座位
        for (TrainCarriage carriage : carriageList) {
            Integer rowCount = carriage.getRowCount();
            String seatType = carriage.getSeatType();
            int seatIndex = 1; // 同车厢座序从 1 开始

            // 根据座位类型获取列枚举列表（如一等座对应 ACDF 列）
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(seatType);
            if (colEnumList.isEmpty()) {
                LOG.warn("座位类型 [{}] 未配置列信息，跳过车厢 [{}]", seatType, carriage.getIndex());
                continue;
            }

            LOG.info("生成车厢 [{}]（座位类型：{}，行数：{}）的座位", carriage.getIndex(), seatType, rowCount);

            // 4. 生成每一排、每一列的座位
            for (int row = 1; row <= rowCount; row++) {
                String twoDigitRow = StrUtil.fillBefore(String.valueOf(row), '0', 2); // 转为两位字符串（如 01, 02）
                for (SeatColEnum colEnum : colEnumList) {
                    TrainSeat trainSeat = new TrainSeat();
                    trainSeat.setId(SnowUtil.getSnowflakeNextId());
                    trainSeat.setTrainCode(trainCode);
                    trainSeat.setCarriageIndex(carriage.getIndex()); // 车厢号
                    trainSeat.setRow(twoDigitRow); // 排号（如 "01"）
                    trainSeat.setCol(colEnum.getCode()); // 列号（如 "A"）
                    trainSeat.setSeatType(seatType); // 座位类型（如 "1" 表示一等座）
                    trainSeat.setCarriageSeatIndex(seatIndex++); // 同车厢座序自动递增
                    trainSeat.setCreateTime(now);
                    trainSeat.setUpdateTime(now);

                    // 插入座位数据
                    TrainSeatMapper.insert(trainSeat);
                }
            }
            LOG.info("车厢 [{}] 座位生成完成，共生成 {} 个座位", carriage.getIndex(), rowCount * colEnumList.size());
        }

        LOG.info("车次 [{}] 座位生成完成，共生成 {} 个座位",
                trainCode,
                carriageList.stream().mapToInt(c -> c.getRowCount() * SeatColEnum.getColsByType(c.getSeatType()).size()).sum()
        );
    }
}
