package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.DailyTrainStation;
import com.chovysun.train.business.req.DailyTrainStationQueryReq;
import com.chovysun.train.business.req.DailyTrainStationSaveReq;
import com.chovysun.train.business.resp.DailyTrainStationQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.Date;
import java.util.List;

public interface IDailyTrainStationService extends IService<DailyTrainStation> {

    void save(DailyTrainStationSaveReq req);

    PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req);

    void delete(Long id);

    List<DailyTrainStationQueryResp> queryByTrain(Date date, String trainCode);
}
