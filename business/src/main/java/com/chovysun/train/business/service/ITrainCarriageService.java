package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.TrainCarriage;
import com.chovysun.train.business.req.TrainCarriageQueryReq;
import com.chovysun.train.business.req.TrainCarriageSaveReq;
import com.chovysun.train.business.resp.TrainCarriageQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.List;

public interface ITrainCarriageService extends IService<TrainCarriage> {

    void save(TrainCarriageSaveReq req);

    PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req);

    void delete(Long id);

    List<TrainCarriage> selectByTrainCode(String trainCode);
}
