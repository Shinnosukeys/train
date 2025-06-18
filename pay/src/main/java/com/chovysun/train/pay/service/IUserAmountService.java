package com.chovysun.train.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chovysun.train.pay.domain.UserAmount;
import java.math.BigDecimal;

public interface IUserAmountService extends IService<UserAmount> {
    /**
     * 根据会员 ID 获取用户金额信息
     * @param memberId 会员 ID
     * @return 用户金额信息
     */
    UserAmount getUserAmountByMemberId(Long memberId);

    /**
     * 扣减用户金额
     * @param memberId 会员 ID
     * @param amount 扣减金额
     * @return 是否扣减成功
     */
    boolean deductAmount(Long memberId, BigDecimal amount);
}
