package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.DailyTrain;
import com.chovysun.train.business.req.DailyTrainQueryReq;
import com.chovysun.train.business.req.DailyTrainSaveReq;
import com.chovysun.train.business.resp.DailyTrainQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.Date;

public interface IDailyTrainService extends IService<DailyTrain> {
    void save(DailyTrainSaveReq req);

    PageResp<DailyTrainQueryResp> queryList(DailyTrainQueryReq req);

    void delete(Long id);

    void genDaily(Date date);

}
