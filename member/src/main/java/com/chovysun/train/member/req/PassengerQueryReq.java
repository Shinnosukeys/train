package com.chovysun.train.member.req;

import com.chovysun.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PassengerQueryReq extends PageReq {

    private Long memberId;
}
