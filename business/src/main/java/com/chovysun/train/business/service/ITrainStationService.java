package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.TrainStation;
import com.chovysun.train.business.req.TrainStationQueryReq;
import com.chovysun.train.business.req.TrainStationSaveReq;
import com.chovysun.train.business.resp.TrainStationQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.List;

public interface ITrainStationService extends IService<TrainStation> {

    void save(TrainStationSaveReq req);

    PageResp<TrainStationQueryResp> queryList(TrainStationQueryReq req);

    void delete(Long id);

    List<TrainStationQueryResp> queryAll();
}
