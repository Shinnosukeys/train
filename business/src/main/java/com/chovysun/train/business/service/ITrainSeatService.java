package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.TrainSeat;
import com.chovysun.train.business.req.TrainSeatQueryReq;
import com.chovysun.train.business.req.TrainSeatSaveReq;
import com.chovysun.train.business.resp.TrainSeatQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.List;

public interface ITrainSeatService extends IService<TrainSeat> {

    void save(TrainSeatSaveReq req);

    PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req);

    void delete(Long id);

    List<TrainSeat> selectByTrainCode(String trainCode);

    void genTrainSeat(String trainCode);
}
