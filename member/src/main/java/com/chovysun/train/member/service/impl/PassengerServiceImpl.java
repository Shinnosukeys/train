package com.chovysun.train.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.common.context.LoginMemberContext;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import com.chovysun.train.member.domain.Passenger;
import com.chovysun.train.member.mapper.PassengerMapper;
import com.chovysun.train.member.req.PassengerQueryReq;
import com.chovysun.train.member.req.PassengerSaveReq;
import com.chovysun.train.member.resp.PassengerQueryResp;
import com.chovysun.train.member.service.IPassengerService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, Passenger> implements IPassengerService {

    @Resource
    private PassengerMapper passengerMapper;

    private static final Logger LOG = LoggerFactory.getLogger(PassengerServiceImpl.class);


    @Override
    public void save(PassengerSaveReq req) {
        DateTime dataTime = DateTime.now();
        Passenger passenger = BeanUtil.copyProperties(req, Passenger.class);
        if (ObjectUtil.isNull(passenger.getId())) {
            passenger.setMemberId(LoginMemberContext.getId());
            passenger.setId(SnowUtil.getSnowflakeNextId());
            passenger.setCreateTime(dataTime);
            passenger.setUpdateTime(dataTime);
            passengerMapper.insert(passenger);
        } else {
            passenger.setUpdateTime(dataTime);
            passengerMapper.updateById(passenger);
        }
    }

    @Override
    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<Passenger> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("member_id", req.getMemberId());

        // 分页查询
        Page<Passenger> page = new Page<>(req.getPage(), req.getSize());
        IPage<Passenger> passengerPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", passengerPage.getTotal());
        LOG.info("总页数：{}", passengerPage.getPages());

        // 转换为响应对象
        List<PassengerQueryResp> list = BeanUtil.copyToList(passengerPage.getRecords(), PassengerQueryResp.class);

        PageResp<PassengerQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(passengerPage.getTotal());
        pageResp.setList(list);
        return pageResp;
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
