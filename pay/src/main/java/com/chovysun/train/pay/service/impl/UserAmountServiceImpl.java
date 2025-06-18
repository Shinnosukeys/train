package com.chovysun.train.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.pay.domain.UserAmount;
import com.chovysun.train.pay.mapper.UserAmountMapper;
import com.chovysun.train.pay.service.IUserAmountService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserAmountServiceImpl extends ServiceImpl<UserAmountMapper, UserAmount> implements IUserAmountService {
    @Resource
    private UserAmountMapper userAmountMapper;

    @Override
    public UserAmount getUserAmountByMemberId(Long memberId) {
        return userAmountMapper.selectOne(new UpdateWrapper<UserAmount>().eq("member_id", memberId));
    }

    @Override
    public boolean deductAmount(Long memberId, BigDecimal amount) {
        UserAmount userAmount = getUserAmountByMemberId(memberId);
        if (userAmount != null && userAmount.getAmount().compareTo(amount) >= 0) {
            BigDecimal newAmount = userAmount.getAmount().subtract(amount);
            UpdateWrapper<UserAmount> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("member_id", memberId)
                    .set("amount", newAmount)
                    .set("update_time", new java.util.Date());
            int rows = userAmountMapper.update(null, updateWrapper);
            return rows > 0;
        }
        return false;
    }
}
