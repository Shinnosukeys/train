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
import com.chovysun.train.member.domain.Member;
import com.chovysun.train.member.domain.Passenger;
import com.chovysun.train.member.enums.PassengerTypeEnum;
import com.chovysun.train.member.mapper.MemberMapper;
import com.chovysun.train.member.mapper.PassengerMapper;
import com.chovysun.train.member.req.PassengerQueryReq;
import com.chovysun.train.member.req.PassengerSaveReq;
import com.chovysun.train.member.resp.PassengerQueryResp;
import com.chovysun.train.member.service.IPassengerService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, Passenger> implements IPassengerService {

    @Resource
    private PassengerMapper passengerMapper;

    @Resource
    private MemberMapper memberMapper;

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
        removeById(id);
    }

    @Override
    public List<PassengerQueryResp> queryMine() {
        QueryWrapper<Passenger> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("member_id", LoginMemberContext.getId()).orderByAsc("name");

        // 执行查询，获取乘客实体列表
        List<Passenger> passengerList = passengerMapper.selectList(queryWrapper);

        // 将实体列表转换为响应对象列表
        return passengerList.stream()
                .map(passenger -> {
                    PassengerQueryResp resp = new PassengerQueryResp();
                    // 复制属性（根据实际字段调整）
                    BeanUtil.copyProperties(passenger, resp);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void init() {
        LOG.info("开始初始化常用乘客数据...");
        DateTime now = DateTime.now();

        // 查找手机号为13000000000的会员
        QueryWrapper<Member> memberQueryWrapper = new QueryWrapper<>();
        memberQueryWrapper.eq("mobile", "13000000000");
        Member member = memberMapper.selectOne(memberQueryWrapper);

        // 如果会员不存在，创建新会员
        if (member == null) {
            LOG.info("未找到测试会员，创建手机号为13000000000的会员");
            member = new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile("13000000000");
            memberMapper.insert(member);
        }

        // 定义默认的常用乘客列表
        List<String> nameList = Arrays.asList("张三", "李四", "王五");
        List<Passenger> passengerList = new ArrayList<>();

        // 检查每个乘客是否已存在，不存在则添加
        for (String name : nameList) {
            QueryWrapper<Passenger> passengerQueryWrapper = new QueryWrapper<>();
            passengerQueryWrapper
                    .eq("member_id", member.getId())
                    .eq("name", name);

            // 检查乘客是否存在
            long count = passengerMapper.selectCount(passengerQueryWrapper);
            if (count == 0) {
                LOG.info("为会员 {} 添加乘客 {}", member.getId(), name);
                Passenger passenger = new Passenger();
                passenger.setId(SnowUtil.getSnowflakeNextId());
                passenger.setMemberId(member.getId());
                passenger.setName(name);
                passenger.setIdCard("123456789123456789"); // 测试用身份证号
                passenger.setType(PassengerTypeEnum.ADULT.getCode());
                passenger.setCreateTime(now);
                passenger.setUpdateTime(now);
                passengerMapper.insert(passenger);
            } else {
                LOG.info("乘客 {} 已存在，跳过添加", name);
            }
        }

        LOG.info("常用乘客数据初始化完成");
    }
}
