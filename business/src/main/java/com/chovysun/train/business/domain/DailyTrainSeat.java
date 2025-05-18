package com.chovysun.train.business.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class DailyTrainSeat {
    private Long id;

    private Date date;

    private String trainCode;

    private Integer carriageIndex;

    // TODO 避免使用数据库保留关键字作为字段名，提升代码可读性和兼容性。
    @TableField("`row`") // 添加反引号
    private String row;

    @TableField("`col`") // 添加反引号
    private String col;

    private String seatType;

    private Integer carriageSeatIndex;

    private String sell;

    private Date createTime;

    private Date updateTime;
}