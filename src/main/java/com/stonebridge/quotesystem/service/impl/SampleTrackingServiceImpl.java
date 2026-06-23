package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public Page<SampleTracking> getListPage(Integer current, Integer size, String keyword, List<String> statusList) {
        QueryWrapper<SampleTracking> wrapper = new QueryWrapper<>();

        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wrapper.and(w -> w.eq("creator", currentUser).or().isNull("creator"));

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("customer_info", keyword).or().like("remarks", keyword));
        }
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in("status", statusList);
        }

        List<SampleTracking> list = trackingMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();

        // 原有的核心排序算法保持不变
        for (SampleTracking item : list) {
            boolean isOverdue = item.getPlanDate().isBefore(today);
            item.setIsOverdue(isOverdue);

            if ("MAKING".equals(item.getStatus())) {
                if (!isOverdue) {
                    item.setSortGroup(1);
                } else {
                    item.setSortGroup(2);
                }
            } else if ("SHIPPED".equals(item.getStatus())) {
                item.setSortGroup(3);
            } else {
                item.setSortGroup(4);
            }
        }

        list.sort((a, b) -> {
            if (!a.getSortGroup().equals(b.getSortGroup())) {
                return a.getSortGroup().compareTo(b.getSortGroup());
            }
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

        // ★ 新增：内存分页逻辑
        int total = list.size();
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<SampleTracking> pageList;
        if (fromIndex >= total) {
            pageList = new java.util.ArrayList<>();
        } else {
            pageList = list.subList(fromIndex, toIndex);
        }

        // 封装为 Page 对象返回
        Page<SampleTracking> page = new Page<>(current, size, total);
        page.setRecords(pageList);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(SampleSaveDTO dto) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        SampleTracking tracking;
        if (dto.getId() == null) {
            // 新增
            tracking = new SampleTracking();
            tracking.setCustomerInfo(dto.getCustomerInfo());
            tracking.setPlanDate(dto.getPlanDate());
            tracking.setRemarks(dto.getRemarks());
            tracking.setStatus("MAKING");
            tracking.setCreator(currentUser);
            tracking.setCreateTime(LocalDateTime.now());
            // ★ 新增：新建时同步写入更新时间
            tracking.setUpdateTime(LocalDateTime.now());
            trackingMapper.insert(tracking);
        } else {
            // 更新
            tracking = trackingMapper.selectById(dto.getId());
            if (tracking == null) {
                throw new RuntimeException("数据不存在");
            }
            if (dto.getRemarks() != null) tracking.setRemarks(dto.getRemarks());
            if (dto.getStatus() != null) {
                tracking.setStatus(dto.getStatus());
                if ("SHIPPED".equals(dto.getStatus())) tracking.setTrackingNo(dto.getTrackingNo());
                if ("ENDED".equals(dto.getStatus()) && tracking.getEndTime() == null) {
                    tracking.setEndTime(LocalDateTime.now());
                }
            }
            // ★ 新增：只要发生修改，就刷新更新时间
            tracking.setUpdateTime(LocalDateTime.now());
            trackingMapper.updateById(tracking);
        }

        imageMapper.delete(new QueryWrapper<SampleImage>().eq("sample_id", tracking.getId()));

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