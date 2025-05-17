package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.TrainStation;
import com.chovysun.train.business.mapper.TrainStationMapper;
import com.chovysun.train.business.req.TrainStationQueryReq;
import com.chovysun.train.business.req.TrainStationSaveReq;
import com.chovysun.train.business.resp.TrainStationQueryResp;
import com.chovysun.train.business.service.ITrainStationService;
import com.chovysun.train.common.exception.BusinessException;
import com.chovysun.train.common.exception.BusinessExceptionEnum;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainStationServiceImpl extends ServiceImpl<TrainStationMapper, TrainStation> implements ITrainStationService {

    @Resource
    private TrainStationMapper trainStationMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);

    @Override
    public void save(TrainStationSaveReq req) {
        DateTime dataTime = DateTime.now();
        TrainStation trainStation = BeanUtil.copyProperties(req, TrainStation.class);
        if (ObjectUtil.isNull(trainStation.getId())) {

            // 保存之前，先校验唯一键是否存在
            TrainStation trainStationDB = selectByUnique(req.getTrainCode(), req.getIndex());
            if (ObjectUtil.isNotEmpty(trainStationDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_INDEX_UNIQUE_ERROR);
            }
            // 保存之前，先校验唯一键是否存在
            trainStationDB = selectByUnique(req.getTrainCode(), req.getName());
            if (ObjectUtil.isNotEmpty(trainStationDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_NAME_UNIQUE_ERROR);
            }

            trainStation.setId(SnowUtil.getSnowflakeNextId());
            trainStation.setCreateTime(dataTime);
            trainStation.setUpdateTime(dataTime);
            trainStationMapper.insert(trainStation);
        } else {
            trainStation.setUpdateTime(dataTime);
            trainStationMapper.updateById(trainStation);
        }
    }

    private TrainStation selectByUnique(String trainCode, Integer index) {
        QueryWrapper<TrainStation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("train_code", trainCode)
                .eq("`index`", index);
        return trainStationMapper.selectOne(queryWrapper);
    }

    private TrainStation selectByUnique(String trainCode, String name) {
        QueryWrapper<TrainStation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("train_code", trainCode)
                .eq("name", name);
        return trainStationMapper.selectOne(queryWrapper);
    }

    @Override
    public PageResp<TrainStationQueryResp> queryList(TrainStationQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<TrainStation> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByAsc("train_code", "`index`"); // `index` 加反引号避免关键字问题

        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }

        // 分页查询
        Page<TrainStation> page = new Page<>(req.getPage(), req.getSize());
        IPage<TrainStation> trainStationPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainStationPage.getTotal());
        LOG.info("总页数：{}", trainStationPage.getPages());

        // 转换为响应对象
        List<TrainStationQueryResp> list = BeanUtil.copyToList(trainStationPage.getRecords(), TrainStationQueryResp.class);

        PageResp<TrainStationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainStationPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<TrainStationQueryResp> queryAll() {
        QueryWrapper<TrainStation> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("code");
        List<TrainStation> trainStations = baseMapper.selectList(queryWrapper);

        return BeanUtil.copyToList(trainStations, TrainStationQueryResp.class);
    }
}
