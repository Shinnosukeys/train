package com.chovysun.train.business.enums;

public enum ConfirmOrderStatusEnum {

    INIT("I", "初始"),
    PENDING("P", "处理中"),
    SUCCESS("S", "下单成功"),
    FAILURE("F", "下单失败"),
    SUCCESS_PAY("S", "支付成功"),
    FAILURE_PAY("F", "支付失败"),

    EMPTY("E", "无票"),
    CANCEL("C", "取消");

    private String code;

    private String desc;

    ConfirmOrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override    public String toString() {
        return "ConfirmOrderStatusEnum{" +
                "code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                "} " + super.toString();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
