package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.Train;
import com.chovysun.train.business.req.TrainQueryReq;
import com.chovysun.train.business.req.TrainSaveReq;
import com.chovysun.train.business.resp.TrainQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.List;

public interface ITrainService extends IService<Train> {

    void save(TrainSaveReq req);

    PageResp<TrainQueryResp> queryList(TrainQueryReq req);

    void delete(Long id);

    List<TrainQueryResp> queryAll();

    List<Train> selectAll();
}
