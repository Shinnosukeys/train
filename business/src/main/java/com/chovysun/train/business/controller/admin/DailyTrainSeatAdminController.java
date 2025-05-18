package com.chovysun.train.business.controller.admin;

import com.chovysun.train.business.req.DailyTrainSeatQueryReq;
import com.chovysun.train.business.req.DailyTrainSeatSaveReq;
import com.chovysun.train.business.resp.DailyTrainSeatQueryResp;
import com.chovysun.train.business.service.IDailyTrainSeatService;
import com.chovysun.train.common.resp.CommonResp;
import com.chovysun.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/daily-train-seat")
public class DailyTrainSeatAdminController {

    @Resource
    private IDailyTrainSeatService dailyTrainSeatService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody DailyTrainSeatSaveReq req) {
        dailyTrainSeatService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainSeatQueryResp>> queryList(@Valid DailyTrainSeatQueryReq req) {
        PageResp<DailyTrainSeatQueryResp> list = dailyTrainSeatService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        dailyTrainSeatService.delete(id);
        return new CommonResp<>();
    }

}
