package com.chovysun.train.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.member.domain.Member;
import com.chovysun.train.member.req.MemberLoginReq;
import com.chovysun.train.member.req.MemberRegisterReq;
import com.chovysun.train.member.req.MemberSendCodeReq;
import com.chovysun.train.member.resp.MemberLoginResp;

public interface IMemberService extends IService<Member> {
    long register(MemberRegisterReq req);

    void sendCode(MemberSendCodeReq req);

    MemberLoginResp login(MemberLoginReq req);
}
