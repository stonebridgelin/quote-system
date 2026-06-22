package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.entity.SampleImage;
import com.stonebridge.quotesystem.entity.SampleTracking;
import com.stonebridge.quotesystem.entity.dto.SampleSaveDTO;
import com.stonebridge.quotesystem.mapper.SampleImageMapper;
import com.stonebridge.quotesystem.mapper.SampleTrackingMapper;
import com.stonebridge.quotesystem.service.ISampleTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SampleTrackingServiceImpl implements ISampleTrackingService {

    @Autowired
    private SampleTrackingMapper trackingMapper;

    @Autowired
    private SampleImageMapper imageMapper;

    @Override
    public List<SampleTracking> getList(String keyword, List<String> statusList) {
        QueryWrapper<SampleTracking> wrapper = new QueryWrapper<>();

        // 当前登录人隔离
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wrapper.eq("creator", currentUser);

        // 模糊搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("customer_info", keyword).or().like("remarks", keyword));
        }
        // 多状态筛选
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in("status", statusList);
        }

        List<SampleTracking> list = trackingMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();

        // 核心排序算法：按照用户要求的层级进行组别划分
        for (SampleTracking item : list) {
            boolean isOverdue = item.getPlanDate().isBefore(today);
            item.setIsOverdue(isOverdue);

            if ("MAKING".equals(item.getStatus())) {
                if (!isOverdue) {
                    item.setSortGroup(1); // 最上面：制作中(未逾期)
                } else {
                    item.setSortGroup(2); // 中间：制作中(逾期)
                }
            } else if ("SHIPPED".equals(item.getStatus())) {
                item.setSortGroup(3); // 偏下：已打包寄出
            } else {
                item.setSortGroup(4); // 最下面：已结束
            }
        }

        list.sort((a, b) -> {
            // 优先按组别排序
            if (!a.getSortGroup().equals(b.getSortGroup())) {
                return a.getSortGroup().compareTo(b.getSortGroup());
            }
            // 组内排序逻辑
            if (a.getSortGroup() == 1) {
                return a.getCreateTime().compareTo(b.getCreateTime());
            } else if (a.getSortGroup() == 2) {
                return a.getPlanDate().compareTo(b.getPlanDate());
            } else if (a.getSortGroup() == 4) {
                if (a.getEndTime() == null) return 1;
                if (b.getEndTime() == null) return -1;
                return b.getEndTime().compareTo(a.getEndTime());
            }
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // ★ 事务控制移交到 Service 层
    public void saveOrUpdate(SampleSaveDTO dto) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        SampleTracking tracking;
        if (dto.getId() == null) {
            // 新增
            tracking = new SampleTracking();
            tracking.setCustomerInfo(dto.getCustomerInfo());
            tracking.setPlanDate(dto.getPlanDate());
            tracking.setRemarks(dto.getRemarks());
            tracking.setStatus("MAKING"); // 新建自动转为制作中
            tracking.setCreator(currentUser);
            tracking.setCreateTime(LocalDateTime.now());
            trackingMapper.insert(tracking);
        } else {
            // 更新
            tracking = trackingMapper.selectById(dto.getId());
            if (tracking == null) {
                throw new RuntimeException("数据不存在"); // 抛出异常由 Controller 捕获
            }
            if (dto.getRemarks() != null) tracking.setRemarks(dto.getRemarks());
            if (dto.getStatus() != null) {
                tracking.setStatus(dto.getStatus());
                if ("SHIPPED".equals(dto.getStatus())) tracking.setTrackingNo(dto.getTrackingNo());
                if ("ENDED".equals(dto.getStatus()) && tracking.getEndTime() == null) {
                    tracking.setEndTime(LocalDateTime.now());
                }
            }
            trackingMapper.updateById(tracking);
        }

        // 全量删除该样品单下的旧图片，防止重复累加
        imageMapper.delete(new QueryWrapper<SampleImage>().eq("sample_id", tracking.getId()));

        // 处理新传入的图片
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (String base64 : dto.getImages()) {
                SampleImage image = new SampleImage();
                image.setSampleId(tracking.getId());
                image.setBase64Data(base64);
                imageMapper.insert(image);
            }
        }
    }

    @Override
    public SampleSaveDTO getDetail(Long id) {
        SampleTracking tracking = trackingMapper.selectById(id);
        if (tracking == null) {
            throw new RuntimeException("数据不存在");
        }

        SampleSaveDTO dto = new SampleSaveDTO();
        dto.setId(tracking.getId());
        dto.setCustomerInfo(tracking.getCustomerInfo());
        dto.setPlanDate(tracking.getPlanDate());
        dto.setRemarks(tracking.getRemarks());
        dto.setStatus(tracking.getStatus());
        dto.setTrackingNo(tracking.getTrackingNo());

        // 查询并封装图片 Base64 数据
        List<SampleImage> images = imageMapper.selectList(new QueryWrapper<SampleImage>().eq("sample_id", id));
        dto.setImages(images.stream().map(SampleImage::getBase64Data).collect(Collectors.toList()));

        return dto;
    }
}