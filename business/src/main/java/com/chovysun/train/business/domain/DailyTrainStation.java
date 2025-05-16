package com.chovysun.train.business.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class DailyTrainStation {
    private Long id;

    private Date date;

    private String trainCode;

    private Integer index;

    private String name;

    private String namePinyin;

    private Date inTime;

    private Date outTime;

    private Date stopTime;

    private BigDecimal km;

    private Date createTime;

    private Date updateTime;
}