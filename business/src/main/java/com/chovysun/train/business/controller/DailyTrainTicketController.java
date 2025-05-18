package com.chovysun.train.business.controller;

import com.chovysun.train.business.req.DailyTrainTicketQueryReq;
import com.chovysun.train.business.resp.DailyTrainTicketQueryResp;
import com.chovysun.train.business.service.IDailyTrainTicketService;
import com.chovysun.train.common.resp.CommonResp;
import com.chovysun.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/daily-train-ticket")
public class DailyTrainTicketController {

    @Resource
    private IDailyTrainTicketService dailyTrainTicketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryList(req);
        return new CommonResp<>(list);
    }

}
