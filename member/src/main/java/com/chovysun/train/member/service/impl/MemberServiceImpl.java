package com.chovysun.train.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.common.exception.BusinessException;
import com.chovysun.train.common.exception.BusinessExceptionEnum;
import com.chovysun.train.common.util.JwtUtil;
import com.chovysun.train.common.util.SnowUtil;
import com.chovysun.train.member.domain.Member;
import com.chovysun.train.member.mapper.MemberMapper;
import com.chovysun.train.member.req.MemberLoginReq;
import com.chovysun.train.member.req.MemberRegisterReq;
import com.chovysun.train.member.req.MemberSendCodeReq;
import com.chovysun.train.member.resp.MemberLoginResp;
import com.chovysun.train.member.service.IMemberService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Resource
    private MemberMapper memberMapper;

    private static final Logger LOG = LoggerFactory.getLogger(MemberServiceImpl.class);

    /**
     * 手机号注册功能
     * 如果手机号已注册，会抛出BusinessException异常
     * @param req 待注册的手机号
     * @return 新注册会员的ID
     */
    @Override
    public long register(MemberRegisterReq req) {
        String mobile = req.getMobile();
        Member existMember = memberMapper.selectOne(new QueryWrapper<Member>().eq("mobile", mobile));
        if (existMember != null) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }

        Member member = new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }

    @Override
    public void sendCode(MemberSendCodeReq req) {
        String mobile = req.getMobile();
        Member existMember = memberMapper.selectOne(new QueryWrapper<Member>().eq("mobile", mobile));

        // 如果手机号不存在，则插入一条记录
        if (existMember == null) {
            LOG.info("手机号不存在，插入一条记录");
            Member member = new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(mobile);
            memberMapper.insert(member);
        } else {
            LOG.info("手机号存在，不插入记录");
        }

        // 生成验证码
        // String code = RandomUtil.randomString(4);
        String code = "8888";
        LOG.info("生成短信验证码：{}", code);

        // 保存短信记录表：手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间
        LOG.info("保存短信记录表");

        // 对接短信通道，发送短信
        LOG.info("对接短信通道");
    }

    public MemberLoginResp login(MemberLoginReq req) {
        String mobile = req.getMobile();
        String code = req.getCode();
        Member existMember = memberMapper.selectOne(new QueryWrapper<Member>().eq("mobile", mobile));

        // 如果手机号不存在，则插入一条记录
        if (existMember == null) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_NOT_EXIST);
        }

        // 校验短信验证码
        if (!"8888".equals(code)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_ERROR);
        }

        MemberLoginResp memberLoginResp = BeanUtil.copyProperties(existMember, MemberLoginResp.class);
        String token = JwtUtil.createToken(memberLoginResp.getId(), memberLoginResp.getMobile());
        memberLoginResp.setToken(token);
        return memberLoginResp;
    }
}
