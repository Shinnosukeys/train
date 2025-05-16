package com.chovysun.train.business.domain;

import lombok.Data;

import java.util.Date;

@Data
public class DailyTrainSeat {
    private Long id;

    private Date date;

    private String trainCode;

    private Integer carriageIndex;

    private String row;

    private String col;

    private String seatType;

    private Integer carriageSeatIndex;

    private String sell;

    private Date createTime;

    private Date updateTime;
}