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
import com.chovysun.train.business.domain.DailyTrainCarriage;
import com.chovysun.train.business.domain.TrainCarriage;
import com.chovysun.train.business.mapper.DailyTrainCarriageMapper;
import com.chovysun.train.business.req.DailyTrainCarriageQueryReq;
import com.chovysun.train.business.req.DailyTrainCarriageSaveReq;
import com.chovysun.train.business.resp.DailyTrainCarriageQueryResp;
import com.chovysun.train.business.service.IDailyTrainCarriageService;
import com.chovysun.train.business.service.ITrainCarriageService;
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
public class DailyTrainCarriageServiceImpl extends ServiceImpl<DailyTrainCarriageMapper, DailyTrainCarriage> implements IDailyTrainCarriageService {

    @Resource
    private DailyTrainCarriageMapper dailyTrainCarriageMapper;

    @Resource
    private ITrainCarriageService trainCarriageService;

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainCarriageServiceImpl.class);

    @Override
    public void save(DailyTrainCarriageSaveReq req) {
        DateTime dataTime = DateTime.now();
        DailyTrainCarriage DailyTrainCarriage = BeanUtil.copyProperties(req, DailyTrainCarriage.class);
        if (ObjectUtil.isNull(DailyTrainCarriage.getId())) {
            DailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            DailyTrainCarriage.setCreateTime(dataTime);
            DailyTrainCarriage.setUpdateTime(dataTime);
            dailyTrainCarriageMapper.insert(DailyTrainCarriage);
        } else {
            DailyTrainCarriage.setUpdateTime(dataTime);
            dailyTrainCarriageMapper.updateById(DailyTrainCarriage);
        }
    }

    @Override
    public PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<DailyTrainCarriage> queryWrapper = new QueryWrapper<>();

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
        Page<DailyTrainCarriage> page = new Page<>(req.getPage(), req.getSize());
        IPage<DailyTrainCarriage> trainPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainPage.getTotal());
        LOG.info("总页数：{}", trainPage.getPages());

        // 转换为响应对象
        List<DailyTrainCarriageQueryResp> list = BeanUtil.copyToList(trainPage.getRecords(), DailyTrainCarriageQueryResp.class);

        PageResp<DailyTrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Transactional
    public void genDailyTrainCarriage(Date date, String trainCode) {
        LOG.info("开始生成日期【{}】车次【{}】的车厢信息", date, trainCode);

        // 删除改日的座位信息
        QueryWrapper<DailyTrainCarriage> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("date", date).eq("train_code", trainCode);
        remove(deleteWrapper);
        LOG.info("已清空 日期【{}】车次【{}】原有的车厢记录", date, trainCode);

        // 查出某车次的所有的座位信息
        List<TrainCarriage> trainCarriageList = trainCarriageService.selectByTrainCode(trainCode);
        if (CollUtil.isEmpty(trainCarriageList)) {
            LOG.info("该车次没有车厢基础数据，生成该车次的车厢信息结束");
            return;
        }

        // 根据基础座位信息生成每日座位信息
        for (TrainCarriage trainCarriage : trainCarriageList) {
            DateTime now = DateTime.now();
            DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(trainCarriage, DailyTrainCarriage.class);
            dailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriage.setDate(date);
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        }

        LOG.info("生成日期【{}】车次【{}】的座位信息结束", DateUtil.formatDate(date), trainCode);
    }

    public List<DailyTrainCarriage> selectBySeatType(Date date, String trainCode, String seatType) {
        QueryWrapper<DailyTrainCarriage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", date).
                eq("train_code", trainCode).
                eq("seat_type", seatType);
        return dailyTrainCarriageMapper.selectList(queryWrapper);
    }
}
