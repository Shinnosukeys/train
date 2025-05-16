package com.chovysun.train.business.domain;

import lombok.Data;

import java.util.Date;

@Data
public class DailyTrainCarriage {
    private Long id;

    private Date date;

    private String trainCode;

    private Integer index;

    private String seatType;

    private Integer seatCount;

    private Integer rowCount;

    private Integer colCount;

    private Date createTime;

    private Date updateTime;
}