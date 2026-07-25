package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.SampleImage;
import com.stonebridge.quotesystem.business.entity.SampleTracking;
import com.stonebridge.quotesystem.business.entity.dto.SampleSaveDTO;
import com.stonebridge.quotesystem.business.mapper.SampleImageMapper;
import com.stonebridge.quotesystem.business.mapper.SampleTrackingMapper;
import com.stonebridge.quotesystem.business.service.ISampleTrackingService;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SampleTrackingServiceImpl implements ISampleTrackingService {

    private static final String STATUS_MAKING = "MAKING";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_ENDED = "ENDED";

    private final SampleTrackingMapper trackingMapper;

    private final SampleImageMapper imageMapper;

    @Override
    public Page<SampleTracking> getListPage(Integer current, Integer size, String keyword, List<String> statusList) {
        QueryWrapper<SampleTracking> wrapper = new QueryWrapper<>();

        String currentUser = SecurityUtil.getCurrentUserId();
        wrapper.and(w -> w.eq("creator", currentUser).or().isNull("creator"));

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("customer_info", keyword).or().like("remarks", keyword));
        }
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in("status", statusList);
        }

        List<SampleTracking> list = trackingMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();

        for (SampleTracking item : list) {
            boolean isOverdue = item.getPlanDate() != null && item.getPlanDate().isBefore(today);
            item.setIsOverdue(isOverdue);

            if (STATUS_MAKING.equals(item.getStatus())) {
                item.setSortGroup(isOverdue ? 2 : 1);
            } else if (isCompletedStatus(item.getStatus())) {
                // SHIPPED 是当前完成状态，ENDED 仅兼容历史数据，二者统一排序。
                item.setSortGroup(3);
            } else {
                // 防御历史异常状态，正常保存流程不会再产生其他状态。
                item.setSortGroup(4);
            }
        }

        list.sort((a, b) -> {
            if (!a.getSortGroup().equals(b.getSortGroup())) {
                return a.getSortGroup().compareTo(b.getSortGroup());
            }
            if (Integer.valueOf(1).equals(a.getSortGroup())) {
                return compareDateTimeAscending(a.getCreateTime(), b.getCreateTime());
            } else if (Integer.valueOf(2).equals(a.getSortGroup())) {
                return compareDateAscending(a.getPlanDate(), b.getPlanDate());
            } else if (Integer.valueOf(3).equals(a.getSortGroup())) {
                return compareDateTimeDescending(completedSortTime(a), completedSortTime(b));
            }
            return compareDateTimeDescending(a.getCreateTime(), b.getCreateTime());
        });

        int total = list.size();
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<SampleTracking> pageList;
        if (fromIndex >= total) {
            pageList = new java.util.ArrayList<>();
        } else {
            pageList = list.subList(fromIndex, toIndex);
        }

        Page<SampleTracking> page = new Page<>(current, size, total);
        page.setRecords(pageList);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(SampleSaveDTO dto) {
        String currentUser = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        SampleTracking tracking;
        if (dto.getId() == null) {
            tracking = new SampleTracking();
            tracking.setCustomerInfo(dto.getCustomerInfo());
            tracking.setPlanDate(dto.getPlanDate());
            tracking.setRemarks(dto.getRemarks());
            // 新建样品单固定从制作中开始，不接受前端指定完成状态。
            tracking.setStatus(STATUS_MAKING);
            tracking.setCreator(currentUser);
            tracking.setCreateTime(now);
            tracking.setUpdateTime(now);
            tracking.setEndTime(null);
            trackingMapper.insert(tracking);
        } else {
            tracking = trackingMapper.selectById(dto.getId());
            if (tracking == null) {
                throw new BusinessException(404, "数据不存在");
            }
            if (dto.getRemarks() != null) {
                tracking.setRemarks(dto.getRemarks());
            }
            applyStatusUpdate(tracking, dto, now);
            tracking.setUpdateTime(now);
            trackingMapper.updateById(tracking);
        }

        // 主表与图片仍在同一个事务中，任一图片保存失败都会整体回滚。
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

    @Override
    public void removeById(Long id) {
        trackingMapper.deleteById(id);
    }

    private void applyStatusUpdate(SampleTracking tracking, SampleSaveDTO dto, LocalDateTime now) {
        String requestedStatus = normalizeRequestedStatus(dto.getStatus());
        String currentStatus = tracking.getStatus();

        if (requestedStatus == null) {
            updateCompletedTrackingNoIfProvided(tracking, dto.getTrackingNo());
            return;
        }

        validateStatus(requestedStatus);
        if (isCompletedStatus(currentStatus)) {
            updateCompletedRecord(tracking, requestedStatus, dto.getTrackingNo(), now);
            return;
        }

        if (STATUS_MAKING.equals(requestedStatus)) {
            tracking.setStatus(STATUS_MAKING);
            return;
        }

        if (STATUS_SHIPPED.equals(requestedStatus)) {
            tracking.setTrackingNo(resolveRequiredTrackingNo(dto.getTrackingNo(), tracking.getTrackingNo()));
            tracking.setStatus(STATUS_SHIPPED);
            setCompletionTimeOnce(tracking, now);
            return;
        }

        // ENDED 仅用于兼容旧客户端，新的正常流程应提交 SHIPPED。
        tracking.setStatus(STATUS_ENDED);
        updateTrackingNoIfProvided(tracking, dto.getTrackingNo());
        setCompletionTimeOnce(tracking, now);
    }

    private void updateCompletedRecord(SampleTracking tracking, String requestedStatus,
                                       String requestedTrackingNo, LocalDateTime now) {
        if (STATUS_MAKING.equals(requestedStatus)) {
            throw new BusinessException(400, "已完成的样品单不能退回制作中");
        }

        // 已完成记录保持原有完成状态，避免 SHIPPED 与历史 ENDED 互相转换。
        if (STATUS_SHIPPED.equals(requestedStatus)) {
            tracking.setTrackingNo(resolveRequiredTrackingNo(requestedTrackingNo, tracking.getTrackingNo()));
        } else {
            updateTrackingNoIfProvided(tracking, requestedTrackingNo);
        }
        setCompletionTimeOnce(tracking, now);
    }

    private void updateCompletedTrackingNoIfProvided(SampleTracking tracking, String requestedTrackingNo) {
        if (isCompletedStatus(tracking.getStatus())) {
            updateTrackingNoIfProvided(tracking, requestedTrackingNo);
        }
    }

    private void updateTrackingNoIfProvided(SampleTracking tracking, String requestedTrackingNo) {
        if (requestedTrackingNo == null) {
            return;
        }
        String normalizedTrackingNo = requestedTrackingNo.trim();
        if (normalizedTrackingNo.isEmpty()) {
            throw new BusinessException(400, "快递单号不能为空");
        }
        tracking.setTrackingNo(normalizedTrackingNo);
    }

    private String resolveRequiredTrackingNo(String requestedTrackingNo, String existingTrackingNo) {
        String effectiveTrackingNo = requestedTrackingNo != null ? requestedTrackingNo : existingTrackingNo;
        if (effectiveTrackingNo == null || effectiveTrackingNo.trim().isEmpty()) {
            throw new BusinessException(400, "快递单号不能为空");
        }
        return effectiveTrackingNo.trim();
    }

    private void setCompletionTimeOnce(SampleTracking tracking, LocalDateTime now) {
        if (tracking.getEndTime() == null) {
            tracking.setEndTime(now);
        }
    }

    private String normalizeRequestedStatus(String status) {
        if (status == null) {
            return null;
        }
        return status.trim();
    }

    private void validateStatus(String status) {
        if (!STATUS_MAKING.equals(status)
                && !STATUS_SHIPPED.equals(status)
                && !STATUS_ENDED.equals(status)) {
            throw new BusinessException(400, "样品状态不合法");
        }
    }

    private static boolean isCompletedStatus(String status) {
        return STATUS_SHIPPED.equals(status) || STATUS_ENDED.equals(status);
    }

    private static LocalDateTime completedSortTime(SampleTracking tracking) {
        if (tracking.getEndTime() != null) {
            return tracking.getEndTime();
        }
        if (tracking.getUpdateTime() != null) {
            return tracking.getUpdateTime();
        }
        return tracking.getCreateTime();
    }

    private static int compareDateAscending(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static int compareDateTimeAscending(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static int compareDateTimeDescending(LocalDateTime left, LocalDateTime right) {
        return compareDateTimeAscending(right, left);
    }
}
