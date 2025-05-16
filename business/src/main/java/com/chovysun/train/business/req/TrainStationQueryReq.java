package com.chovysun.train.business.req;

import com.chovysun.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TrainStationQueryReq extends PageReq {

    private String trainCode;
}
