package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.DailyTrainSeat;
import com.chovysun.train.business.req.DailyTrainSeatQueryReq;
import com.chovysun.train.business.req.DailyTrainSeatSaveReq;
import com.chovysun.train.business.resp.DailyTrainSeatQueryResp;
import com.chovysun.train.common.resp.PageResp;

public interface IDailyTrainSeatService extends IService<DailyTrainSeat> {

    void save(DailyTrainSeatSaveReq req);

    PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req);

    void delete(Long id);
}
