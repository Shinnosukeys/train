package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.Station;
import com.chovysun.train.business.mapper.StationMapper;
import com.chovysun.train.business.req.StationQueryReq;
import com.chovysun.train.business.req.StationSaveReq;
import com.chovysun.train.business.resp.StationQueryResp;
import com.chovysun.train.business.service.IStationService;
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
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService {

    @Resource
    private StationMapper stationMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StationServiceImpl.class);


    @Override
    public void save(StationSaveReq req) {
        DateTime dataTime = DateTime.now();
        Station station = BeanUtil.copyProperties(req, Station.class);
        if (ObjectUtil.isNull(station.getId())) {

            Station stationDB = selectByUnique(req.getName());
            if (ObjectUtil.isNotEmpty(stationDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_NAME_UNIQUE_ERROR);
            }

            station.setId(SnowUtil.getSnowflakeNextId());
            station.setCreateTime(dataTime);
            station.setUpdateTime(dataTime);
            stationMapper.insert(station);
        } else {
            station.setUpdateTime(dataTime);
            stationMapper.updateById(station);
        }
    }

    private Station selectByUnique(String name) {
        QueryWrapper<Station> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        return stationMapper.selectOne(queryWrapper);
    }

    @Override
    public PageResp<StationQueryResp> queryList(StationQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<Station> queryWrapper = new QueryWrapper<>();

        // 分页查询
        Page<Station> page = new Page<>(req.getPage(), req.getSize());
        IPage<Station> stationPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", stationPage.getTotal());
        LOG.info("总页数：{}", stationPage.getPages());

        // 转换为响应对象
        List<StationQueryResp> list = BeanUtil.copyToList(stationPage.getRecords(), StationQueryResp.class);

        PageResp<StationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(stationPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<StationQueryResp> queryAll() {
        QueryWrapper<Station> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("name_pinyin");

        List<Station> stations = baseMapper.selectList(queryWrapper);

        return BeanUtil.copyToList(stations, StationQueryResp.class);
    }
}
