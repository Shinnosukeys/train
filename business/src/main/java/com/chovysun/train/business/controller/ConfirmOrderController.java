package com.chovysun.train.business.controller;

import com.chovysun.train.business.req.ConfirmOrderDoReq;
import com.chovysun.train.business.service.IConfirmOrderService;
import com.chovysun.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


// http://localhost:8000/business/confirm-order/query-line-count/null
//请求方法
//GET


@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderController.class);

    @Resource
    private IConfirmOrderService confirmOrderService;

    // 接口的资源名称不要和接口路径一致，会导致限流后走不到降级方法中
    //@SentinelResource(value = "confirmOrderDo", blockHandler = "doConfirmBlock")
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) {
        confirmOrderService.doConfirm(req);
        return new CommonResp<>();

        //        if (!env.equals("dev")) {
//            // 图形验证码校验
//            String imageCodeToken = req.getImageCodeToken();
//            String imageCode = req.getImageCode();
//            String imageCodeRedis = redisTemplate.opsForValue().get(imageCodeToken);
//            LOG.info("从redis中获取到的验证码：{}", imageCodeRedis);
//            if (ObjectUtils.isEmpty(imageCodeRedis)) {
//                return new CommonResp<>(false, "验证码已过期", null);
//            }
//            // 验证码校验，大小写忽略，提升体验，比如Oo Vv Ww容易混
//            if (!imageCodeRedis.equalsIgnoreCase(imageCode)) {
//                return new CommonResp<>(false, "验证码不正确", null);
//            } else {
//                // 验证通过后，移除验证码
//                redisTemplate.delete(imageCodeToken);
//            }
//        }
//
//        Long id = beforeConfirmOrderService.beforeDoConfirm(req);
//        return new CommonResp<>(String.valueOf(id));
    }

    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id) {
        Integer count = confirmOrderService.queryLineCount(id);
        return new CommonResp<>(count);
    }
//
//    @GetMapping("/cancel/{id}")
//    public CommonResp<Integer> cancel(@PathVariable Long id) {
//        Integer count = confirmOrderService.cancel(id);
//        return new CommonResp<>(count);
//    }
//
//    /** 降级方法，需包含限流方法的所有参数和BlockException参数，且返回值要保持一致
//     * @param req
//     * @param e
//     */
//    public CommonResp<Object> doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
//        LOG.info("ConfirmOrderController购票请求被限流：{}", req);
//        // throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
//        CommonResp<Object> commonResp = new CommonResp<>();
//        commonResp.setSuccess(false);
//        commonResp.setMessage(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION.getDesc());
//        return commonResp;
//
//    }

}
