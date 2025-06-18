package com.chovysun.train.pay.controller;

import com.chovysun.train.pay.service.IUserAmountService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/order_pay")
public class OrderPayController {

    @Resource
    private IUserAmountService userAmountService;

    /**
     * 支付接口
     * @param memberId 会员 ID
     * @param amount 支付金额
     * @return 支付结果
     */
    @PostMapping("/pay")
    public String pay(@RequestParam Long memberId, @RequestParam BigDecimal amount) {
        boolean result = userAmountService.deductAmount(memberId, amount);
        if (result) {
            return "支付成功";
        } else {
            return "支付失败，余额不足";
        }
    }
}
