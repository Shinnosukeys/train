package com.chovysun.train.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.member.domain.Passenger;
import com.chovysun.train.member.req.PassengerQueryReq;
import com.chovysun.train.member.req.PassengerSaveReq;
import com.chovysun.train.member.resp.PassengerQueryResp;

import java.util.List;

public interface IPassengerService extends IService<Passenger> {

    void save(PassengerSaveReq req);

    PageResp<PassengerQueryResp> queryList(PassengerQueryReq req);

    void delete(Long id);

    List<PassengerQueryResp> queryMine();

    void init();
}
