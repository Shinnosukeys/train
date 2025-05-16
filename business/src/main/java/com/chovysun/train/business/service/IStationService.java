package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.Station;
import com.chovysun.train.business.req.StationQueryReq;
import com.chovysun.train.business.req.StationSaveReq;
import com.chovysun.train.business.resp.StationQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.List;

public interface IStationService extends IService<Station> {

    void save(StationSaveReq req);

    PageResp<StationQueryResp> queryList(StationQueryReq req);

    void delete(Long id);

    List<StationQueryResp> queryAll();
}
