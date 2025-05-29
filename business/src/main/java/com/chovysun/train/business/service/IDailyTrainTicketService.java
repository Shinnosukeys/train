package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.DailyTrainTicket;
import com.chovysun.train.business.req.DailyTrainTicketQueryReq;
import com.chovysun.train.business.req.DailyTrainTicketSaveReq;
import com.chovysun.train.business.resp.DailyTrainTicketQueryResp;
import com.chovysun.train.common.resp.PageResp;

public interface IDailyTrainTicketService extends IService<DailyTrainTicket> {

    void save(DailyTrainTicketSaveReq req);

    PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req);

    void delete(Long id);

    PageResp<DailyTrainTicketQueryResp> queryList2(DailyTrainTicketQueryReq req);
}
