package com.chovysun.train.pay.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("user_amount")
public class UserAmount {
    @TableId(type = IdType.AUTO)

    private Long id;

    private Long memberId;

    private BigDecimal amount;

    private Date createTime;

    private Date updateTime;
}
