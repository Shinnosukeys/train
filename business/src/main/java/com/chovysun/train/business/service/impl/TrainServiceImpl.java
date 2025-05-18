package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.Train;
import com.chovysun.train.business.mapper.TrainMapper;
import com.chovysun.train.business.req.TrainQueryReq;
import com.chovysun.train.business.req.TrainSaveReq;
import com.chovysun.train.business.resp.TrainQueryResp;
import com.chovysun.train.business.service.ITrainService;
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
public class TrainServiceImpl extends ServiceImpl<TrainMapper, Train> implements ITrainService {

    @Resource
    private TrainMapper trainMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);


    @Override
    public void save(TrainSaveReq req) {
        DateTime dataTime = DateTime.now();
        Train train = BeanUtil.copyProperties(req, Train.class);
        if (ObjectUtil.isNull(train.getId())) {

            Train trainDB = selectByUnique(req.getCode());
            if (ObjectUtil.isNotEmpty(trainDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CODE_UNIQUE_ERROR);
            }

            train.setId(SnowUtil.getSnowflakeNextId());
            train.setCreateTime(dataTime);
            train.setUpdateTime(dataTime);
            trainMapper.insert(train);
        } else {
            train.setUpdateTime(dataTime);
            trainMapper.updateById(train);
        }
    }


    private Train selectByUnique(String code) {
        QueryWrapper<Train> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        return trainMapper.selectOne(queryWrapper);
    }

    @Override
    public PageResp<TrainQueryResp> queryList(TrainQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<Train> queryWrapper = new QueryWrapper<>();

        // 分页查询
        Page<Train> page = new Page<>(req.getPage(), req.getSize());
        IPage<Train> trainPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", trainPage.getTotal());
        LOG.info("总页数：{}", trainPage.getPages());

        // 转换为响应对象
        List<TrainQueryResp> list = BeanUtil.copyToList(trainPage.getRecords(), TrainQueryResp.class);

        PageResp<TrainQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(trainPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<TrainQueryResp> queryAll() {
        QueryWrapper<Train> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("code");
        List<Train> trains = baseMapper.selectList(queryWrapper);

        return BeanUtil.copyToList(trains, TrainQueryResp.class);
    }


    @Override
    public List<Train> selectAll() {
        QueryWrapper<Train> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("code");
        return trainMapper.selectList(queryWrapper);
    }
}
