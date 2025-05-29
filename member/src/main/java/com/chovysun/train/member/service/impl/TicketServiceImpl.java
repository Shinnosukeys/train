package com.chovysun.train.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chovysun.train.common.req.MemberTicketReq;
import com.chovysun.train.common.resp.PageResp;
import com.chovysun.train.common.util.SnowUtil;
import com.chovysun.train.member.domain.Ticket;
import com.chovysun.train.member.mapper.TicketMapper;
import com.chovysun.train.member.req.TicketQueryReq;
import com.chovysun.train.member.resp.TicketQueryResp;
import com.chovysun.train.member.service.ITicketService;
import io.seata.core.context.RootContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private TicketMapper ticketMapper;

    private static final Logger LOG = LoggerFactory.getLogger(TicketServiceImpl.class);
    
    @Override
    public void save(MemberTicketReq req) {
        LOG.info("seata全局事务ID save: {}", RootContext.getXID());
        DateTime dataTime = DateTime.now();
        Ticket ticket = BeanUtil.copyProperties(req, Ticket.class);
        ticket.setId(SnowUtil.getSnowflakeNextId());
        ticket.setCreateTime(dataTime);
        ticket.setUpdateTime(dataTime);
        ticketMapper.insert(ticket);
    }

    @Override
    public PageResp<TicketQueryResp> queryList(TicketQueryReq req) {
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());

        // 构建查询条件
        QueryWrapper<Ticket> queryWrapper = new QueryWrapper<>();
        if (ObjUtil.isNotNull(req.getMemberId())) {
            queryWrapper.eq("member_id", req.getMemberId());
        }

        // 分页查询
        Page<Ticket> page = new Page<>(req.getPage(), req.getSize());
        IPage<Ticket> TicketPage = this.page(page, queryWrapper);

        LOG.info("总行数：{}", TicketPage.getTotal());
        LOG.info("总页数：{}", TicketPage.getPages());

        // 转换为响应对象
        List<TicketQueryResp> list = BeanUtil.copyToList(TicketPage.getRecords(), TicketQueryResp.class);

        PageResp<TicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(TicketPage.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }
}
