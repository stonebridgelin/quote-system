package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.entity.*;
import com.stonebridge.quotesystem.entity.dto.OrderSaveDTO;
import com.stonebridge.quotesystem.entity.dto.RecordSaveDTO;
import com.stonebridge.quotesystem.mapper.*;
import com.stonebridge.quotesystem.service.IOrderMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderMainServiceImpl implements IOrderMainService {

    @Autowired private OrderMainMapper mainMapper;
    @Autowired private OrderDetailMapper detailMapper;
    @Autowired private OrderRecordMapper recordMapper;
    @Autowired private OrderRecordImageMapper imageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(OrderSaveDTO dto) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isUpdate = dto.getId() != null && !dto.getId().isEmpty();

        OrderMain main = dto;
        main.setUpdateTime(LocalDateTime.now());

        if (!isUpdate) {
            main.setCreator(currentUser);
            main.setCreateTime(LocalDateTime.now());
            mainMapper.insert(main);
        } else {
            mainMapper.updateById(main);
            // 更新时，先清空旧的明细
            detailMapper.delete(new QueryWrapper<OrderDetail>().eq("order_id", main.getId()));
        }

        // 保存新的明细 (前端传来的已经计算好 combo 箱数)
        if (dto.getDetailList() != null) {
            for (int i = 0; i < dto.getDetailList().size(); i++) {
                OrderDetail detail = dto.getDetailList().get(i);
                detail.setId(null);
                detail.setOrderId(main.getId());
                detail.setItemIndex(i + 1);
                detailMapper.insert(detail);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(RecordSaveDTO dto) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 查询当前订单已有多少条记录，生成新的序号
        long count = recordMapper.selectCount(new QueryWrapper<OrderRecord>().eq("order_id", dto.getOrderId()));

        OrderRecord record = new OrderRecord();
        record.setOrderId(dto.getOrderId());
        record.setItemIndex((int) count + 1);
        record.setRemark(dto.getRemark());
        record.setCreator(currentUser);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (String base64 : dto.getImages()) {
                OrderRecordImage img = new OrderRecordImage();
                img.setRecordId(record.getId());
                img.setBase64Data(base64);
                img.setCreateTime(LocalDateTime.now());
                imageMapper.insert(img);
            }
        }
    }

    @Override
    public Page<OrderMain> getOrderPage(Integer current, Integer size, String keyword, Integer status, String deliveryDate) {
        QueryWrapper<OrderMain> wrapper = new QueryWrapper<>();

        // 模糊搜索：订单号、客户单号、对接人、备注
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("order_no", keyword)
                    .or().like("po_no", keyword)
                    .or().like("contact", keyword)
                    .or().like("remark", keyword));
        }

        // 状态精准匹配
        if (status != null) {
            wrapper.eq("status", status);
        }

        // 交期匹配
        if (deliveryDate != null && !deliveryDate.trim().isEmpty()) {
            wrapper.eq("delivery_date", deliveryDate);
        }

        // 默认按交期升序（紧急的在前面），再按创建时间降序
        wrapper.orderByAsc("delivery_date").orderByDesc("create_time");

        return mainMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public OrderSaveDTO getOrderDetail(String id) {
        OrderMain main = mainMapper.selectById(id);
        if (main == null) throw new RuntimeException("订单不存在");

        OrderSaveDTO dto = new OrderSaveDTO();
        org.springframework.beans.BeanUtils.copyProperties(main, dto);

        // 附带查出明细并按 item_index 排序
        List<OrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OrderDetail>().eq("order_id", id).orderByAsc("item_index"));
        dto.setDetailList(details);

        return dto;
    }

    @Override
    public List<RecordSaveDTO> getRecordList(String orderId) {
        // 查出该订单所有记录，按时间降序（最新的在上面）
        List<OrderRecord> records = recordMapper.selectList(
                new QueryWrapper<OrderRecord>().eq("order_id", orderId).orderByDesc("create_time"));

        return records.stream().map(record -> {
            RecordSaveDTO dto = new RecordSaveDTO();
            dto.setOrderId(record.getOrderId());
            dto.setRemark(record.getRemark());
            // ★ 此处借用 DTO 传递一下额外的展示字段，你也可以在 DTO 中加 creator 和 createTime 字段

            // 附带查出该记录的图片 Base64
            List<OrderRecordImage> images = imageMapper.selectList(
                    new QueryWrapper<OrderRecordImage>().eq("record_id", record.getId()));
            dto.setImages(images.stream().map(OrderRecordImage::getBase64Data).collect(java.util.stream.Collectors.toList()));

            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void removeById(String id) {
        mainMapper.deleteById(id);
    }
}