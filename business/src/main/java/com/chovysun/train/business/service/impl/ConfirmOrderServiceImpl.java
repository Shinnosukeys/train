package com.chovysun.train.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.business.domain.ConfirmOrder;
import com.chovysun.train.business.domain.DailyTrainCarriage;
import com.chovysun.train.business.domain.DailyTrainSeat;
import com.chovysun.train.business.domain.DailyTrainTicket;
import com.chovysun.train.business.dto.ConfirmOrderMQDto;
import com.chovysun.train.business.enums.ConfirmOrderStatusEnum;
import com.chovysun.train.business.enums.RedisKeyPreEnum;
import com.chovysun.train.business.enums.SeatColEnum;
import com.chovysun.train.business.enums.SeatTypeEnum;
import com.chovysun.train.business.mapper.ConfirmOrderMapper;
import com.chovysun.train.business.req.ConfirmOrderDoReq;
import com.chovysun.train.business.req.ConfirmOrderQueryReq;
import com.chovysun.train.business.req.ConfirmOrderTicketReq;
import com.chovysun.train.business.resp.ConfirmOrderQueryResp;
import com.chovysun.train.business.service.IConfirmOrderService;
import com.chovysun.train.common.exception.BusinessException;
import com.chovysun.train.common.exception.BusinessExceptionEnum;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.chovysun.train.business.constant.constant.CONFIRM_ORDER_QUEUE;

@Service
public class ConfirmOrderServiceImpl extends ServiceImpl<ConfirmOrderMapper, ConfirmOrder> implements IConfirmOrderService {
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketServiceImpl dailyTrainTicketServiceImpl;

    @Resource
    private DailyTrainCarriageServiceImpl dailyTrainCarriageServiceImpl;

    @Resource
    private DailyTrainSeatServiceImpl dailyTrainSeatServiceImpl;

    @Resource
    private AfterConfirmOrderServiceImpl afterConfirmOrderService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private SkTokenServiceImpl skTokenService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private DailyTrainTicketServiceImpl dailyTrainTicketService;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderServiceImpl.class);

    @Override
    public void save(ConfirmOrderDoReq req) {
        DateTime dataTime = DateTime.now();
        ConfirmOrder ConfirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(ConfirmOrder.getId())) {
            ConfirmOrder.setId(SnowUtil.getSnowflakeNextId());
            ConfirmOrder.setCreateTime(dataTime);
            ConfirmOrder.setUpdateTime(dataTime);
            confirmOrderMapper.insert(ConfirmOrder);
        } else {
            ConfirmOrder.setUpdateTime(dataTime);
            confirmOrderMapper.updateById(ConfirmOrder);
        }
    }

    @Override
    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<ConfirmOrder> queryWrapper = new QueryWrapper<>();

        // 添加排序条件
        queryWrapper.orderByDesc("id");

        // 分页查询
        Page<ConfirmOrder> page = new Page<>(req.getPage(), req.getSize());
        IPage<ConfirmOrder> confirmOrderPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", confirmOrderPage.getTotal());
        LOG.info("总页数：{}", confirmOrderPage.getPages());

        // 转换为响应对象
        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderPage.getRecords(), ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(confirmOrderPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }


    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlock")
    @RabbitListener(queues = CONFIRM_ORDER_QUEUE)
    public void doConfirm(ConfirmOrderMQDto dto) {
        LOG.info("成功发起异步调用-----------------------------------------------------------------------------------------------------");
        MDC.put("LOG_ID", dto.getLogId());
        LOG.info("异步出票开始：{}", dto);

        // 获取分布式锁
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-" + DateUtil.formatDate(dto.getDate()) + "-" + dto.getTrainCode();
        Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(setIfAbsent)) {
            LOG.info("恭喜，抢到锁了！lockKey：{}", lockKey);
        } else {
            LOG.info("没抢到锁，有其它消费线程正在出票，不做任何处理");
            return;
        }
        try {
            while (true) {
                // 取确认订单表的记录，同日期车次，状态是I，分页处理，每次取N条
                LambdaQueryWrapper<ConfirmOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ConfirmOrder::getDate, dto.getDate())
                        .eq(ConfirmOrder::getTrainCode, dto.getTrainCode())
                        .eq(ConfirmOrder::getStatus, ConfirmOrderStatusEnum.INIT.getCode())
                        .orderByAsc(ConfirmOrder::getId);

                // 使用MyBatis Plus的分页
                Page<ConfirmOrder> page = new Page<>(1, 5);
                Page<ConfirmOrder> resultPage = confirmOrderMapper.selectPage(page, queryWrapper);
                List<ConfirmOrder> list = resultPage.getRecords();
                if (CollUtil.isEmpty(list)) {
                    LOG.info("没有需要处理的订单，结束循环");
                    break;
                } else {
                    LOG.info("本次处理{}条订单", list.size());
                }

                // 一条一条的卖
                list.forEach(confirmOrder -> {
                    try {
                        sell(confirmOrder);
                    } catch (BusinessException e) {
                        if (e.getE().equals(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR)) {
                            LOG.info("本订单余票不足，继续售卖下一个订单");
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateStatus(confirmOrder);
                        } else {
                            throw e;
                        }
                    }
                });
            }
        } finally {
            LOG.info("购票流程结束，释放锁！lockKey：{}", lockKey);
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 更新状态
     * @param confirmOrder
     */
    public void updateStatus(ConfirmOrder confirmOrder) {
        UpdateWrapper<ConfirmOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", confirmOrder.getId())
                .set("update_time", new Date())
                .set("status", confirmOrder.getStatus());

        confirmOrderMapper.update(null, updateWrapper);
    }

    /**
     * 售票
     * @param confirmOrder
     */
    private void sell(ConfirmOrder confirmOrder) {
        // 为了演示排队效果，每次出票增加200毫秒延时
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        // 构造ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(confirmOrder.getDate());
        req.setTrainCode(confirmOrder.getTrainCode());
        req.setStart(confirmOrder.getStart());
        req.setEnd(confirmOrder.getEnd());
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));
        req.setImageCode("");
        req.setImageCodeToken("");
        req.setLogId("");

        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        // 将订单设置成处理中，避免重复处理
        LOG.info("将确认订单更新成处理中，避免重复处理，confirm_order.id: {}", confirmOrder.getId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateStatus(confirmOrder);

        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> tickets = req.getTickets();

        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        LOG.info("查出余票记录：{}", dailyTrainTicket);

        // 预扣减余票数量，并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);

        // 最终的选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        // 计算相对第一个座位的偏移值
        // 比如选择的是C1,D2，则偏移值是：[0,5]
        // 比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if (StrUtil.isNotBlank(ticketReq0.getSeat())) {
            LOG.info("本次购票有选座");
            // 查出本次选座的座位类型都有哪些列，用于计算所选座位与第一个座位的偏离值
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            LOG.info("本次选座的座位类型包含的列：{}", colEnumList);

            List<String> referSeatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            LOG.info("用于作参照的两排座位：{}", referSeatList);

            List<Integer> offsetList = new ArrayList<>();
            // 绝对偏移值，即：在参照座位列表中的位置
            List<Integer> aboluteOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                int index = referSeatList.indexOf(ticketReq.getSeat());
                aboluteOffsetList.add(index);
            }
            LOG.info("计算得到所有座位的绝对偏移值：{}", aboluteOffsetList);
            for (Integer index : aboluteOffsetList) {
                int offset = index - aboluteOffsetList.get(0);
                offsetList.add(offset);
            }
            LOG.info("计算得到所有座位的相对第一个座位的偏移值：{}", offsetList);

            getSeat(finalSeatList,
                    date,
                    trainCode,
                    ticketReq0.getSeatTypeCode(),
                    ticketReq0.getSeat().split("")[0], // 从A1得到A
                    offsetList,
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex()
            );

        } else {
            LOG.info("本次购票没有选座");
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                getSeat(finalSeatList,
                        date,
                        trainCode,
                        ticketReq.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex()
                );
            }
        }

        LOG.info("最终选座：{}", finalSeatList);

        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, tickets, confirmOrder);
        } catch (Exception e) {
            LOG.error("保存购票信息失败", e);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
    }


    private static void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticketReq : req.getTickets()) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countLeft);
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countLeft);
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countLeft);
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countLeft);
                }
            }
        }
    }


    /**
     * 挑座位，如果有选座，则一次性挑完，如果无选座，则一个一个挑
     * @param date
     * @param trainCode
     * @param seatType
     * @param column
     * @param offsetList
     */
    private void getSeat(List<DailyTrainSeat> finalSeatList, Date date, String trainCode, String seatType, String column, List<Integer> offsetList, Integer startIndex, Integer endIndex) {
        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageServiceImpl.selectBySeatType(date, trainCode, seatType);
        LOG.info("共查出{}个符合条件的车厢", carriageList.size());

        // 一个车箱一个车箱的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : carriageList) {
            LOG.info("开始从车厢{}选座", dailyTrainCarriage.getIndex());
            getSeatList = new ArrayList<>();
            List<DailyTrainSeat> seatList = dailyTrainSeatServiceImpl.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            LOG.info("车厢{}的座位数：{}", dailyTrainCarriage.getIndex(), seatList.size());

            for (int i = 0; i < seatList.size(); i++) {
                DailyTrainSeat dailyTrainSeat = seatList.get(i);
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
                String col = dailyTrainSeat.getCol();

                // 判断当前座位不能被选中过
                boolean alreadyChooseFlag = false;
                for (DailyTrainSeat finalSeat : finalSeatList){
                    if (finalSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadyChooseFlag = true;
                        break;
                    }
                }
                if (alreadyChooseFlag) {
                    LOG.info("座位{}被选中过，不能重复选中，继续判断下一个座位", seatIndex);
                    continue;
                }

                // 判断column，有值的话要比对列号
                if (StrUtil.isBlank(column)) {
                    LOG.info("无选座");
                } else {
                    if (!column.equals(col)) {
                        LOG.info("座位{}列值不对，继续判断下一个座位，当前列值：{}，目标列值：{}", seatIndex, col, column);
                        continue;
                    }
                }

                boolean isChoose = calSell(dailyTrainSeat, startIndex, endIndex);
                if (isChoose) {
                    LOG.info("选中座位");
                    getSeatList.add(dailyTrainSeat);
                } else {
                    continue;
                }

                // 根据offset选剩下的座位
                boolean isGetAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetList)) {
                    LOG.info("有偏移值：{}，校验偏移的座位是否可选", offsetList);
                    // 从索引1开始，索引0就是当前已选中的票
                    for (int j = 1; j < offsetList.size(); j++) {
                        Integer offset = offsetList.get(j);
                        // 座位在库的索引是从1开始
                        // int nextIndex = seatIndex + offset - 1;
                        int nextIndex = i + offset;

                        // 有选座时，一定是在同一个车箱
                        if (nextIndex >= seatList.size()) {
                            LOG.info("座位{}不可选，偏移后的索引超出了这个车箱的座位数", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }

                        DailyTrainSeat nextDailyTrainSeat = seatList.get(nextIndex);
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
                            LOG.info("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            getSeatList.add(nextDailyTrainSeat);
                        } else {
                            LOG.info("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (!isGetAllOffsetSeat) {
                    getSeatList = new ArrayList<>();
                    continue;
                }

                // 保存选好的座位
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 计算某座位在区间内是否可卖
     * 例：sell=10001，本次购买区间站1~4，则区间已售000
     * 全部是0，表示这个区间可买；只要有1，就表示区间内已售过票
     *
     * 选中后，要计算购票后的sell，比如原来是10001，本次购买区间站1~4
     * 方案：构造本次购票造成的售卖信息01110，和原sell 10001按位与，最终得到11111
     */
    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        // 00001, 00000
        String sell = dailyTrainSeat.getSell();
        //  000, 000
        String sellPart = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(sellPart) > 0) {
            LOG.info("座位{}在本次车站区间{}~{}已售过票，不可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        } else {
            LOG.info("座位{}在本次车站区间{}~{}未售过票，可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            //  111,   111
            String curSell = sellPart.replace('0', '1');
            // 0111,  0111
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            // 01110, 01110
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());

            // 当前区间售票信息curSell 01110与库里的已售信息sell 00001按位与，即可得到该座位卖出此票后的售票详情
            // 15(01111), 14(01110 = 01110|00000)
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            //  1111,  1110
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            // 01111, 01110
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.info("座位{}被选中，原售票信息：{}，车站区间：{}~{}，即：{}，最终售票信息：{}"
                    , dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, curSell, newSell);
            dailyTrainSeat.setSell(newSell);
            return true;

        }
    }

    /**
     * 查询前面有几个人在排队
     * @param id
     */
    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectById(id);
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case PENDING -> 0; // 排队0
            case SUCCESS -> -1; // 成功
            case FAILURE -> -2; // 失败
            case EMPTY -> -3; // 无票
            case CANCEL -> -4; // 取消
            case INIT -> 999; // 需要查表得到实际排队数量
        };

        if (result == 999) {
            // 使用LambdaQueryWrapper构建查询条件
            LambdaQueryWrapper<ConfirmOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    // 筛选同一天、同一车次的订单
                    .eq(ConfirmOrder::getDate, confirmOrder.getDate())
                    .eq(ConfirmOrder::getTrainCode, confirmOrder.getTrainCode())

                    // 只统计创建时间更早的订单
                    .lt(ConfirmOrder::getCreateTime, confirmOrder.getCreateTime())

                    // 状态必须是INIT或PENDING
                    .and(wrapper -> wrapper
                            .eq(ConfirmOrder::getStatus, ConfirmOrderStatusEnum.INIT.getCode())
                            .or()
                            .eq(ConfirmOrder::getStatus, ConfirmOrderStatusEnum.PENDING.getCode())
                    );

            return Math.toIntExact(confirmOrderMapper.selectCount(queryWrapper));
        } else {
            return result;
        }
    }

    /**
     * 取消排队，只有I状态才能取消排队，所以按状态更新
     * @param id
     */
    @Override
    public Integer cancel(Long id) {
        // 创建更新条件：ID等于传入值且状态为INIT
        QueryWrapper<ConfirmOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("status", ConfirmOrderStatusEnum.INIT.getCode());

        // 创建更新内容：将状态设置为CANCEL
        UpdateWrapper<ConfirmOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", ConfirmOrderStatusEnum.CANCEL.getCode())
                .eq("id", id)
                .eq("status", ConfirmOrderStatusEnum.INIT.getCode());

        // 执行更新操作并返回受影响的行数
        return confirmOrderMapper.update(null, updateWrapper);
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public void doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
