package com.chovysun.train.member.controller;

import com.chovysun.train.common.resp.CommonResp;
import com.chovysun.train.member.req.MemberLoginReq;
import com.chovysun.train.member.req.MemberRegisterReq;
import com.chovysun.train.member.req.MemberSendCodeReq;
import com.chovysun.train.member.resp.MemberLoginResp;
import com.chovysun.train.member.service.IMemberService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Resource
    private IMemberService memberService;

    @GetMapping("/count")
    public CommonResp<Long> count() {
        long count = memberService.count();
        return new CommonResp<>(count);
    }

    @PostMapping("/register")
    public CommonResp<Long> regitser(@Valid MemberRegisterReq req) {
        long register = memberService.register(req);
        return new CommonResp<>(register);
    }

    @PostMapping("/send-code")
    public CommonResp<Long> sendCode(@Valid @RequestBody MemberSendCodeReq req) {
        memberService.sendCode(req);
        return new CommonResp<>();
    }

    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid @RequestBody MemberLoginReq req) {
        MemberLoginResp resp = memberService.login(req);
        return new CommonResp<>(resp);
    }
}
