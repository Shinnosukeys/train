package com.chovysun.train.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.business.domain.SkToken;
import com.chovysun.train.business.req.SkTokenQueryReq;
import com.chovysun.train.business.req.SkTokenSaveReq;
import com.chovysun.train.business.resp.SkTokenQueryResp;
import com.chovysun.train.common.resp.PageResp;

import java.util.Date;

public interface ISkTokenService extends IService<SkToken> {

    void save(SkTokenSaveReq req);

    PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req);

    void delete(Long id);

    boolean validSkToken(Date date, String trainCode, Long memberId);

    void genDaily(Date date, String trainCode);
}
