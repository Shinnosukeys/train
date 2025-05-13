package com.chovysun.train.common.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResp<T> implements Serializable {

    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页的列表
     */
    private List<T> list;

//    public Long getTotal() {
//        return total;
//    }
//
//    public void setTotal(Long total) {
//        this.total = total;
//    }
//
//    public List<T> getList() {
//        return list;
//    }
//
//    public void setList(List<T> list) {
//        this.list = list;
//    }
//
//    @Override
//    public String toString() {
//        return "PageResp{" +
//                "total=" + total +
//                ", list=" + list +
//                '}';
//    }
}
