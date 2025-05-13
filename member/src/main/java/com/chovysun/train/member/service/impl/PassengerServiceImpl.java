package com.chovysun.train.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import com.chovysun.train.member.domain.Passenger;
import com.chovysun.train.member.mapper.PassengerMapper;
import com.chovysun.train.member.req.PassengerQueryReq;
import com.chovysun.train.member.req.PassengerSaveReq;
import com.chovysun.train.member.resp.PassengerQueryResp;
import com.chovysun.train.member.service.IPassengerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, Passenger> implements IPassengerService {

    @Resource
    private PassengerMapper passengerMapper;

    @Override
    public void save(PassengerSaveReq req) {
        DateTime dataTime = DateTime.now();
        Passenger passenger = BeanUtil.copyProperties(req, Passenger.class);
        passenger.setId(SnowUtil.getSnowflakeNextId());
        passenger.setCreateTime(dataTime);
        passenger.setUpdateTime(dataTime);
        passengerMapper.insert(passenger);
    }

    @Override
    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public List<PassengerQueryResp> queryMine() {
        return null;
    }

    @Override
    public void init() {

    }
}
