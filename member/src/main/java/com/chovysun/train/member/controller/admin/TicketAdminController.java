package com.chovysun.train.member.controller.admin;

import com.chovysun.train.common.resp.CommonResp;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.member.req.TicketQueryReq;
import com.chovysun.train.member.resp.TicketQueryResp;
import com.chovysun.train.member.service.ITicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ticket")
public class TicketAdminController {

    @Resource
    private ITicketService ticketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>> queryList(@Valid TicketQueryReq req) {
        PageResp<TicketQueryResp> list = ticketService.queryList(req);
        return new CommonResp<>(list);
    }

}
