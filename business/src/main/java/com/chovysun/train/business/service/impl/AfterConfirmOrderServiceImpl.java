package com.chovysun.train.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chovysun.train.business.domain.ConfirmOrder;
import com.chovysun.train.business.domain.DailyTrainSeat;
import com.chovysun.train.business.domain.DailyTrainTicket;
import com.chovysun.train.business.enums.ConfirmOrderStatusEnum;
import com.chovysun.train.business.feign.MemberFeign;
import com.chovysun.train.business.mapper.ConfirmOrderMapper;
import com.chovysun.train.business.mapper.DailyTrainSeatMapper;
import com.chovysun.train.business.mapper.DailyTrainTicketMapper;
import com.chovysun.train.business.req.ConfirmOrderTicketReq;
import com.chovysun.train.common.req.MemberTicketReq;
import com.chovysun.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AfterConfirmOrderServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderServiceImpl.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private DailyTrainTicketMapper dailyTrainTicketMapper;

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private MemberFeign memberFeign;

    /**
     * 选中座位后事务处理：
     *  座位表修改售卖情况sell；
     *  余票详情表修改余票；
     *  为会员增加购票记录
     *  更新确认订单为成功
     */
    // @Transactional
    public void afterDoConfirm(
            DailyTrainTicket dailyTrainTicket,
            List<DailyTrainSeat> finalSeatList,
            List<ConfirmOrderTicketReq> tickets,
            ConfirmOrder confirmOrder
    ) throws Exception {

        for (int j = 0; j < finalSeatList.size(); j++) {
            DailyTrainSeat dailyTrainSeat = finalSeatList.get(j);

            // 创建UpdateWrapper来指定更新条件和要更新的字段
            UpdateWrapper<DailyTrainSeat> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", dailyTrainSeat.getId())
                    .set("sell", dailyTrainSeat.getSell())
                    .set("update_time", new Date());

            // 执行更新操作，第一个参数为null表示仅根据wrapper条件更新
            dailyTrainSeatMapper.update(null, updateWrapper);

            // 计算这个站卖出去后，影响了哪些站的余票库存
            // 参照2-3节 如何保证不超卖、不少卖，还要能承受极高的并发 10:30左右
            // 影响的库存：本次选座之前没卖过票的，和本次购买的区间有交集的区间
            // 假设10个站，本次买4~7站
            // 原售：001000001
            // 购买：000011100
            // 新售：001011101
            // 影响：XXX11111X
            // Integer startIndex = 4;
            // Integer endIndex = 7;
            // Integer minStartIndex = startIndex - 往前碰到的最后一个0;
            // Integer maxStartIndex = endIndex - 1;
            // Integer minEndIndex = startIndex + 1;
            // Integer maxEndIndex = endIndex + 往后碰到的最后一个0;
            Integer startIndex = dailyTrainTicket.getStartIndex();
            Integer endIndex = dailyTrainTicket.getEndIndex();
            char[] chars = dailyTrainSeat.getSell().toCharArray();
            Integer maxStartIndex = endIndex - 1;
            Integer minEndIndex = startIndex + 1;

            Integer minStartIndex = 0;
            for (int i = startIndex - 1; i >= 0; i--) {
                char aChar = chars[i];
                if (aChar == '1') {
                    minStartIndex = i + 1;
                    break;
                }
            }
            LOG.info("影响出发站区间：" + minStartIndex + "-" + maxStartIndex);

            Integer maxEndIndex = dailyTrainSeat.getSell().length();
            for (int i = endIndex; i < dailyTrainSeat.getSell().length(); i++) {
                char aChar = chars[i];
                if (aChar == '1') {
                    maxEndIndex = i;
                    break;
                }
            }
            LOG.info("影响到达站区间：" + minEndIndex + "-" + maxEndIndex);

            // 更新余票库存
            updateTicketCount(
                    dailyTrainSeat.getDate(),
                    dailyTrainSeat.getTrainCode(),
                    dailyTrainSeat.getSeatType(),
                    minStartIndex,
                    maxStartIndex,
                    minEndIndex,
                    maxEndIndex);

//            dailyTrainTicketMapperCust.updateCountBySell(
//                    dailyTrainSeat.getDate(),
//                    dailyTrainSeat.getTrainCode(),
//                    dailyTrainSeat.getSeatType(),
//                    minStartIndex,
//                    maxStartIndex,
//                    minEndIndex,
//                    maxEndIndex);
//
            // 调用会员服务接口，为会员增加一张车票
            MemberTicketReq memberTicketReq = new MemberTicketReq();
            memberTicketReq.setMemberId(confirmOrder.getMemberId());
            memberTicketReq.setPassengerId(tickets.get(j).getPassengerId());
            memberTicketReq.setPassengerName(tickets.get(j).getPassengerName());
            memberTicketReq.setTrainDate(dailyTrainTicket.getDate());
            memberTicketReq.setTrainCode(dailyTrainTicket.getTrainCode());
            memberTicketReq.setCarriageIndex(dailyTrainSeat.getCarriageIndex());
            memberTicketReq.setSeatRow(dailyTrainSeat.getRow());
            memberTicketReq.setSeatCol(dailyTrainSeat.getCol());
            memberTicketReq.setStartStation(dailyTrainTicket.getStart());
            memberTicketReq.setStartTime(dailyTrainTicket.getStartTime());
            memberTicketReq.setEndStation(dailyTrainTicket.getEnd());
            memberTicketReq.setEndTime(dailyTrainTicket.getEndTime());
            memberTicketReq.setSeatType(dailyTrainSeat.getSeatType());
            CommonResp<Object> commonResp = memberFeign.save(memberTicketReq);
            LOG.info("调用member接口，返回：{}", commonResp);

            // 更新订单状态为成功
            UpdateWrapper<ConfirmOrder> updateConfirmOrderWrapper = new UpdateWrapper<>();
            updateConfirmOrderWrapper.eq("id", confirmOrder.getId())
                    .set("update_time", new Date())
                    .set("status", ConfirmOrderStatusEnum.SUCCESS.getCode());
            confirmOrderMapper.update(null, updateConfirmOrderWrapper);


            // 模拟调用方出现异常
            // Thread.sleep(10000);
            // if (1 == 1) {
            //     throw new Exception("测试异常");
            // }
        }
    }



    // 使用MyBatis-Plus实现的余票更新方法
    private void updateTicketCount(Date date, String trainCode, String seatTypeCode,
                                   Integer minStartIndex, Integer maxStartIndex,
                                   Integer minEndIndex, Integer maxEndIndex) {
        LambdaUpdateWrapper<DailyTrainTicket> ticketUpdateWrapper = Wrappers.lambdaUpdate();

        // 根据座位类型更新对应字段
        switch (seatTypeCode) {
            case "1":
                ticketUpdateWrapper.setSql("ydz = ydz - 1");
                break;
            case "2":
                ticketUpdateWrapper.setSql("edz = edz - 1");
                break;
            case "3":
                ticketUpdateWrapper.setSql("rw = rw - 1");
                break;
            case "4":
                ticketUpdateWrapper.setSql("yw = yw - 1");
                break;
            default:
                throw new IllegalArgumentException("未知座位类型: " + seatTypeCode);
        }

        // 设置更新条件
        ticketUpdateWrapper.eq(DailyTrainTicket::getDate, date)
                .eq(DailyTrainTicket::getTrainCode, trainCode)
                .ge(DailyTrainTicket::getStartIndex, minStartIndex)
                .le(DailyTrainTicket::getStartIndex, maxStartIndex)
                .ge(DailyTrainTicket::getEndIndex, minEndIndex)
                .le(DailyTrainTicket::getEndIndex, maxEndIndex);

        dailyTrainTicketMapper.update(null, ticketUpdateWrapper);
    }
}
