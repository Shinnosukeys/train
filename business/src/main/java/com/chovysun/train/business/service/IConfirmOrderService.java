package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.ConfirmOrder;
import com.chovysun.train.business.req.ConfirmOrderDoReq;
import com.chovysun.train.business.req.ConfirmOrderQueryReq;
import com.chovysun.train.business.resp.ConfirmOrderQueryResp;
import com.chovysun.train.common.resp.PageResp;

public interface IConfirmOrderService extends IService<ConfirmOrder>{

    void save(ConfirmOrderDoReq req);

    PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req);

    void delete(Long id);

    void doConfirm(ConfirmOrderDoReq req);

    Integer queryLineCount(Long id);
}
