package com.chovysun.train.business.controller.admin;

import com.chovysun.train.business.req.ConfirmOrderDoReq;
import com.chovysun.train.business.req.ConfirmOrderQueryReq;
import com.chovysun.train.business.resp.ConfirmOrderQueryResp;
import com.chovysun.train.business.service.IConfirmOrderService;
import com.chovysun.train.common.resp.CommonResp;
import com.chovysun.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/confirm-order")
public class ConfirmOrderAdminController {

    @Resource
    private IConfirmOrderService confirmOrderService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody ConfirmOrderDoReq req) {
        confirmOrderService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<ConfirmOrderQueryResp>> queryList(@Valid ConfirmOrderQueryReq req) {
        PageResp<ConfirmOrderQueryResp> list = confirmOrderService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        confirmOrderService.delete(id);
        return new CommonResp<>();
    }

}
