package com.chovysun.train.common.resp;

import lombok.Data;

@Data
public class CommonResp<T> {

    /**
     * 业务上的成功或失败
     */
    private boolean success = true;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回泛型数据，自定义类型
     */
    private T content;

    // 添加带参数的构造函数
    public CommonResp(T content) {
        this.content = content;
    }

    // 保留默认构造函数
    public CommonResp() {}
}
