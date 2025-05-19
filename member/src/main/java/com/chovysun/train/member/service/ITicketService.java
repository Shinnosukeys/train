package com.chovysun.train.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.common.req.MemberTicketReq;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.member.domain.Ticket;
import com.chovysun.train.member.req.TicketQueryReq;
import com.chovysun.train.member.resp.TicketQueryResp;

public interface ITicketService extends IService<Ticket> {

    void save(MemberTicketReq req);
    PageResp<TicketQueryResp> queryList(TicketQueryReq req);

    void delete(Long id);
}
