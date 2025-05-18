package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.TrainCarriage;
import com.chovysun.train.business.enums.SeatColEnum;
import com.chovysun.train.business.mapper.TrainCarriageMapper;
import com.chovysun.train.business.req.TrainCarriageQueryReq;
import com.chovysun.train.business.req.TrainCarriageSaveReq;
import com.chovysun.train.business.resp.TrainCarriageQueryResp;
import com.chovysun.train.business.service.ITrainCarriageService;
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
public class TrainCarriageServiceImpl extends ServiceImpl<TrainCarriageMapper, TrainCarriage> implements ITrainCarriageService {

    @Resource
    private TrainCarriageMapper TrainCarriageMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);

    @Override
    public void save(TrainCarriageSaveReq req) {
        DateTime dataTime = DateTime.now();

        // 自动计算出列数和总座位数
        List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(req.getSeatType());
        req.setColCount(seatColEnums.size());
        req.setSeatCount(req.getColCount() * req.getRowCount());

        TrainCarriage TrainCarriage = BeanUtil.copyProperties(req, TrainCarriage.class);

        if (ObjectUtil.isNull(TrainCarriage.getId())) {

            // 保存之前，先校验唯一键是否存在
            TrainCarriage TrainCarriageDB = selectByUnique(req.getTrainCode(), req.getIndex());
            if (ObjectUtil.isNotEmpty(TrainCarriageDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR);
            }

            TrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            TrainCarriage.setCreateTime(dataTime);
            TrainCarriage.setUpdateTime(dataTime);
            TrainCarriageMapper.insert(TrainCarriage);
        } else {
            TrainCarriage.setUpdateTime(dataTime);
            TrainCarriageMapper.updateById(TrainCarriage);
        }
    }

    private TrainCarriage selectByUnique(String trainCode, Integer index) {
        QueryWrapper<TrainCarriage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("train_code", trainCode)
                .eq("`index`", index);
        return TrainCarriageMapper.selectOne(queryWrapper);
    }

    @Override
    public PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<TrainCarriage> queryWrapper = new QueryWrapper<>();

        // 添加排序条件：按车次升序、序号升序（与原逻辑一致）
        queryWrapper.orderByAsc("train_code", "`index`"); // `index` 加反引号避免关键字问题

        // 添加车次过滤条件（非空时生效）
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            queryWrapper.eq("train_code", req.getTrainCode());
        }

        // 分页查询
        Page<TrainCarriage> page = new Page<>(req.getPage(), req.getSize());
        IPage<TrainCarriage> TrainCarriagePage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", TrainCarriagePage.getTotal());
        LOG.info("总页数：{}", TrainCarriagePage.getPages());

        // 转换为响应对象
        List<TrainCarriageQueryResp> list = BeanUtil.copyToList(TrainCarriagePage.getRecords(), TrainCarriageQueryResp.class);

        PageResp<TrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(TrainCarriagePage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<TrainCarriage> selectByTrainCode(String trainCode) {
        QueryWrapper<TrainCarriage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("train_code", trainCode).orderByAsc("`index`");
        return TrainCarriageMapper.selectList(queryWrapper);
    }
}
