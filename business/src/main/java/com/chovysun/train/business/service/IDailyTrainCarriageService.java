package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.DailyTrainCarriage;
import com.chovysun.train.business.req.DailyTrainCarriageQueryReq;
import com.chovysun.train.business.req.DailyTrainCarriageSaveReq;
import com.chovysun.train.business.resp.DailyTrainCarriageQueryResp;
import com.chovysun.train.common.resp.PageResp;

public interface IDailyTrainCarriageService extends IService<DailyTrainCarriage> {

    void save(DailyTrainCarriageSaveReq req);

    PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req);

    void delete(Long id);
}
