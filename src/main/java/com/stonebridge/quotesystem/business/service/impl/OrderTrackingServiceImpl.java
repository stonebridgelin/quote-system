package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.OrderTracking;
import com.stonebridge.quotesystem.business.entity.OrderTrackingLog;
import com.stonebridge.quotesystem.business.entity.OrderTrackingLogImage;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingLogImageDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingModuleUpdateDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingWaitingDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingLogImageVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingLogVO;
import com.stonebridge.quotesystem.business.enums.TrackModuleEnum;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingLogImageMapper;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingLogMapper;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingService;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderTrackingServiceImpl implements IOrderTrackingService {

    private final OrderTrackingMapper orderTrackingMapper;
    private final OrderTrackingLogMapper orderTrackingLogMapper;
    private final OrderTrackingLogImageMapper orderTrackingLogImageMapper;
    private final IOrderTrackingOptionTranslateService optionTranslateService;
    private final SysUserService sysUserService;

    @Override
    public Page<OrderTrackingListVO> page(OrderTrackingPageQueryDTO queryDTO) {
        Page<OrderTracking> entityPage = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        QueryWrapper<OrderTracking> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);

        // 数据权限：订单追踪业务只能由创建人查看。
        String currentUser = getCurrentUserId();
        wrapper.eq("create_by", currentUser);

        if (hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like("order_no", keyword).or().like("customer_order_no", keyword).or().like("customer_contact", keyword));
        }

        if (queryDTO.getBottomLabel() != null) {
            wrapper.eq("bottom_label", queryDTO.getBottomLabel());
        }
        if (queryDTO.getBottomLabelStatus() != null) {
            wrapper.eq("bottom_label_status", queryDTO.getBottomLabelStatus());
        }

        if (queryDTO.getSticker() != null) {
            wrapper.eq("sticker", queryDTO.getSticker());
        }
        if (queryDTO.getStickerStatus() != null) {
            wrapper.eq("sticker_status", queryDTO.getStickerStatus());
        }

        if (queryDTO.getPrinting() != null) {
            wrapper.eq("printing", queryDTO.getPrinting());
        }
        if (queryDTO.getPrintingStatus() != null) {
            wrapper.eq("printing_status", queryDTO.getPrintingStatus());
        }

        if (queryDTO.getInnerBox() != null) {
            wrapper.eq("inner_box", queryDTO.getInnerBox());
        }
        if (queryDTO.getInnerBoxStatus() != null) {
            wrapper.eq("inner_box_status", queryDTO.getInnerBoxStatus());
        }

        if (queryDTO.getColorBox() != null) {
            wrapper.eq("color_box", queryDTO.getColorBox());
        }
        if (queryDTO.getColorBoxStatus() != null) {
            wrapper.eq("color_box_status", queryDTO.getColorBoxStatus());
        }

        if (queryDTO.getCartonStatus() != null) {
            wrapper.eq("carton_status", queryDTO.getCartonStatus());
        }

        if (queryDTO.getAntiCutBoard() != null) {
            wrapper.eq("anti_cut_board", queryDTO.getAntiCutBoard());
        }

        if (queryDTO.getPlannedProductionDateStart() != null) {
            wrapper.ge("planned_production_date", queryDTO.getPlannedProductionDateStart());
        }
        if (queryDTO.getPlannedProductionDateEnd() != null) {
            wrapper.le("planned_production_date", queryDTO.getPlannedProductionDateEnd());
        }

        if (queryDTO.getPlannedDeliveryDateStart() != null) {
            wrapper.ge("planned_delivery_date", queryDTO.getPlannedDeliveryDateStart());
        }
        if (queryDTO.getPlannedDeliveryDateEnd() != null) {
            wrapper.le("planned_delivery_date", queryDTO.getPlannedDeliveryDateEnd());
        }

        if (queryDTO.getIsFinished() != null) {
            wrapper.eq("is_finished", queryDTO.getIsFinished());
        }

        // 必须在数据库分页前按最后更新时间全局倒序，id 用作相同时间下的稳定次序。
        wrapper.orderByDesc("update_time").orderByDesc("id");

        Page<OrderTracking> rawPage = orderTrackingMapper.selectPage(entityPage, wrapper);

        Page<OrderTrackingListVO> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        List<OrderTrackingListVO> records = rawPage.getRecords().stream()
                .map(optionTranslateService::toListVO)
                .collect(Collectors.toList());
        fillOperatorDisplayNames(records);
        resultPage.setRecords(records);

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(OrderTrackingSaveDTO dto) {
        validateCreate(dto);

        String currentUser = getCurrentUserId();

        OrderTracking order = new OrderTracking();
        BeanUtils.copyProperties(dto, order);

        order.setOrderNo(normalizeOrderNo(dto.getOrderNo()));
        order.setIsDeleted(0);
        order.setIsFinished(defaultInt(dto.getIsFinished(), 0));

        fillDefaultStatus(order);
        fillDefaultWaiting(order);

        order.setCreateTime(LocalDateTime.now());
        order.setCreateBy(currentUser);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(currentUser);

        Long count = orderTrackingMapper.selectCount(new QueryWrapper<OrderTracking>().eq("order_no", order.getOrderNo()).eq("is_deleted", 0));

        if (count != null && count > 0) {
            throw new BusinessException("订单编号已存在");
        }

        orderTrackingMapper.insert(order);

        /*
         * 新增订单时：
         * 1. 不因为默认状态自动生成大量日志；
         * 2. 如果某个模块填写了备注、上传了图片，生成初始日志；
         * 3. 如果某个模块开启了等待回复，也生成初始日志。
         */
        saveCreateInitialLogsIfNeeded(order, dto.getModuleUpdates(), currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OrderTrackingSaveDTO dto) {
        if (!hasText(dto.getId())) {
            throw new BusinessException("订单ID不能为空");
        }

        OrderTracking oldOrder = getOrderOrThrow(dto.getId());
        assertCanEdit(oldOrder);

        String currentUser = getCurrentUserId();

        OrderTracking newOrder = new OrderTracking();
        BeanUtils.copyProperties(oldOrder, newOrder);

        /*
         * 编辑时不允许修改：
         * orderNo、customerOrderNo、customerContact、plannedDeliveryDate、createTime、createBy。
         * 所以这里不要覆盖这些字段。
         */
        newOrder.setShapeDescription(dto.getShapeDescription());
        newOrder.setReviewFormImageUrl(dto.getReviewFormImageUrl());
        newOrder.setReviewFormImageKey(dto.getReviewFormImageKey());

        newOrder.setBottomLabel(dto.getBottomLabel());
        newOrder.setBottomLabelStatus(dto.getBottomLabelStatus());
        newOrder.setIsBottomLabelWaiting(defaultInt(dto.getIsBottomLabelWaiting(), oldOrder.getIsBottomLabelWaiting()));

        newOrder.setSticker(dto.getSticker());
        newOrder.setStickerStatus(dto.getStickerStatus());
        newOrder.setIsStickerWaiting(defaultInt(dto.getIsStickerWaiting(), oldOrder.getIsStickerWaiting()));

        newOrder.setPrinting(dto.getPrinting());
        newOrder.setPrintingStatus(dto.getPrintingStatus());
        newOrder.setPrintingPatternCode(dto.getPrintingPatternCode());
        newOrder.setIsPrintingWaiting(defaultInt(dto.getIsPrintingWaiting(), oldOrder.getIsPrintingWaiting()));

        newOrder.setInnerBox(dto.getInnerBox());
        newOrder.setInnerBoxStatus(dto.getInnerBoxStatus());
        newOrder.setIsInnerBoxWaiting(defaultInt(dto.getIsInnerBoxWaiting(), oldOrder.getIsInnerBoxWaiting()));

        newOrder.setColorBox(dto.getColorBox());
        newOrder.setColorBoxStatus(dto.getColorBoxStatus());
        newOrder.setIsColorBoxWaiting(defaultInt(dto.getIsColorBoxWaiting(), oldOrder.getIsColorBoxWaiting()));

        newOrder.setCartonStatus(dto.getCartonStatus());
        newOrder.setIsCartonWaiting(defaultInt(dto.getIsCartonWaiting(), oldOrder.getIsCartonWaiting()));

        newOrder.setSeparatorType(dto.getSeparatorType());
        newOrder.setAntiCutBoard(dto.getAntiCutBoard());
        newOrder.setPackagingRequirement(dto.getPackagingRequirement());
        newOrder.setPlannedProductionDate(dto.getPlannedProductionDate());
        newOrder.setReturnSample(dto.getReturnSample());
        newOrder.setRemark(dto.getRemark());
        newOrder.setIsFinished(defaultInt(dto.getIsFinished(), 0));

        fillDefaultStatus(newOrder);
        fillDefaultWaiting(newOrder);

        newOrder.setUpdateTime(LocalDateTime.now());
        newOrder.setUpdateBy(currentUser);

        orderTrackingMapper.updateById(newOrder);

        Map<String, OrderTrackingModuleUpdateDTO> updateMap = buildModuleUpdateMap(dto.getModuleUpdates());

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.ORDER.getCode(), null, null, null, null, null, null, updateMap.get(TrackModuleEnum.ORDER.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.BOTTOM_LABEL.getCode(), oldOrder.getBottomLabel(), newOrder.getBottomLabel(), oldOrder.getBottomLabelStatus(), newOrder.getBottomLabelStatus(), null, null, updateMap.get(TrackModuleEnum.BOTTOM_LABEL.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.STICKER.getCode(), oldOrder.getSticker(), newOrder.getSticker(), oldOrder.getStickerStatus(), newOrder.getStickerStatus(), null, null, updateMap.get(TrackModuleEnum.STICKER.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.PRINTING.getCode(), oldOrder.getPrinting(), newOrder.getPrinting(), oldOrder.getPrintingStatus(), newOrder.getPrintingStatus(), oldOrder.getPrintingPatternCode(), newOrder.getPrintingPatternCode(), updateMap.get(TrackModuleEnum.PRINTING.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.INNER_BOX.getCode(), oldOrder.getInnerBox(), newOrder.getInnerBox(), oldOrder.getInnerBoxStatus(), newOrder.getInnerBoxStatus(), null, null, updateMap.get(TrackModuleEnum.INNER_BOX.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.COLOR_BOX.getCode(), oldOrder.getColorBox(), newOrder.getColorBox(), oldOrder.getColorBoxStatus(), newOrder.getColorBoxStatus(), null, null, updateMap.get(TrackModuleEnum.COLOR_BOX.getCode()), currentUser);

        saveModuleLogIfNeeded(oldOrder.getId(), TrackModuleEnum.CARTON.getCode(), null, null, oldOrder.getCartonStatus(), newOrder.getCartonStatus(), null, null, updateMap.get(TrackModuleEnum.CARTON.getCode()), currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.BOTTOM_LABEL.getCode(), oldOrder.getIsBottomLabelWaiting(), newOrder.getIsBottomLabelWaiting(), newOrder.getBottomLabel(), newOrder.getBottomLabelStatus(), null, null, currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.STICKER.getCode(), oldOrder.getIsStickerWaiting(), newOrder.getIsStickerWaiting(), newOrder.getSticker(), newOrder.getStickerStatus(), null, null, currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.PRINTING.getCode(), oldOrder.getIsPrintingWaiting(), newOrder.getIsPrintingWaiting(), newOrder.getPrinting(), newOrder.getPrintingStatus(), newOrder.getPrintingPatternCode(), null, currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.INNER_BOX.getCode(), oldOrder.getIsInnerBoxWaiting(), newOrder.getIsInnerBoxWaiting(), newOrder.getInnerBox(), newOrder.getInnerBoxStatus(), null, null, currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.COLOR_BOX.getCode(), oldOrder.getIsColorBoxWaiting(), newOrder.getIsColorBoxWaiting(), newOrder.getColorBox(), newOrder.getColorBoxStatus(), null, null, currentUser);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), TrackModuleEnum.CARTON.getCode(), oldOrder.getIsCartonWaiting(), newOrder.getIsCartonWaiting(), null, newOrder.getCartonStatus(), null, null, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModule(OrderTrackingModuleUpdateDTO dto) {
        if (!hasText(dto.getOrderId())) {
            throw new BusinessException("订单ID不能为空");
        }
        if (!hasText(dto.getModuleType())) {
            throw new BusinessException("模块类型不能为空");
        }

        TrackModuleEnum moduleEnum = TrackModuleEnum.getByCode(dto.getModuleType());
        if (moduleEnum == null) {
            throw new BusinessException("不支持的追踪模块类型");
        }

        OrderTracking oldOrder = getOrderOrThrow(dto.getOrderId());
        assertCanEdit(oldOrder);

        String currentUser = getCurrentUserId();

        OrderTracking newOrder = new OrderTracking();
        BeanUtils.copyProperties(oldOrder, newOrder);

        switch (moduleEnum) {
            case ORDER:
                // 整单日志不改变主表状态，只记录日志与图片。
                break;
            case BOTTOM_LABEL:
                newOrder.setBottomLabel(dto.getTypeValue());
                newOrder.setBottomLabelStatus(dto.getStatusValue());
                break;
            case STICKER:
                newOrder.setSticker(dto.getTypeValue());
                newOrder.setStickerStatus(dto.getStatusValue());
                break;
            case PRINTING:
                newOrder.setPrinting(dto.getTypeValue());
                newOrder.setPrintingStatus(dto.getStatusValue());
                newOrder.setPrintingPatternCode(dto.getTextValue());
                break;
            case INNER_BOX:
                newOrder.setInnerBox(dto.getTypeValue());
                newOrder.setInnerBoxStatus(dto.getStatusValue());
                break;
            case COLOR_BOX:
                newOrder.setColorBox(dto.getTypeValue());
                newOrder.setColorBoxStatus(dto.getStatusValue());
                break;
            case CARTON:
                newOrder.setCartonStatus(dto.getStatusValue());
                break;
            default:
                throw new BusinessException("不支持的追踪模块类型");
        }

        fillDefaultStatus(newOrder);
        fillDefaultWaiting(newOrder);

        newOrder.setUpdateTime(LocalDateTime.now());
        newOrder.setUpdateBy(currentUser);

        orderTrackingMapper.updateById(newOrder);

        switch (moduleEnum) {
            case ORDER:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), null, null, null, null, null, null, dto, currentUser);
                break;
            case BOTTOM_LABEL:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), oldOrder.getBottomLabel(), newOrder.getBottomLabel(), oldOrder.getBottomLabelStatus(), newOrder.getBottomLabelStatus(), null, null, dto, currentUser);
                break;
            case STICKER:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), oldOrder.getSticker(), newOrder.getSticker(), oldOrder.getStickerStatus(), newOrder.getStickerStatus(), null, null, dto, currentUser);
                break;
            case PRINTING:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), oldOrder.getPrinting(), newOrder.getPrinting(), oldOrder.getPrintingStatus(), newOrder.getPrintingStatus(), oldOrder.getPrintingPatternCode(), newOrder.getPrintingPatternCode(), dto, currentUser);
                break;
            case INNER_BOX:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), oldOrder.getInnerBox(), newOrder.getInnerBox(), oldOrder.getInnerBoxStatus(), newOrder.getInnerBoxStatus(), null, null, dto, currentUser);
                break;
            case COLOR_BOX:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), oldOrder.getColorBox(), newOrder.getColorBox(), oldOrder.getColorBoxStatus(), newOrder.getColorBoxStatus(), null, null, dto, currentUser);
                break;
            case CARTON:
                saveModuleLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), null, null, oldOrder.getCartonStatus(), newOrder.getCartonStatus(), null, null, dto, currentUser);
                break;
            default:
                break;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModuleWaiting(OrderTrackingWaitingDTO dto) {
        if (!hasText(dto.getOrderId())) {
            throw new BusinessException(400, "订单ID不能为空");
        }

        if (!hasText(dto.getModuleType())) {
            throw new BusinessException(400, "模块类型不能为空");
        }

        if (dto.getWaiting() == null || (dto.getWaiting() != 0 && dto.getWaiting() != 1)) {
            throw new BusinessException(400, "等待回复状态不正确");
        }

        TrackModuleEnum moduleEnum = TrackModuleEnum.getByCode(dto.getModuleType());
        if (moduleEnum == null) {
            throw new BusinessException(400, "不支持的追踪模块类型");
        }
        if (TrackModuleEnum.ORDER.equals(moduleEnum)) {
            throw new BusinessException(400, "整单日志不支持等待回复提醒");
        }

        OrderTracking oldOrder = getOrderOrThrow(dto.getOrderId());
        assertCanEdit(oldOrder);

        String currentUser = getCurrentUserId();

        OrderTracking newOrder = new OrderTracking();
        BeanUtils.copyProperties(oldOrder, newOrder);

        Integer beforeWaiting;
        Integer afterWaiting = dto.getWaiting();

        Integer currentType = null;
        Integer currentStatus = null;
        String currentText = null;

        switch (moduleEnum) {
            case BOTTOM_LABEL:
                beforeWaiting = defaultInt(oldOrder.getIsBottomLabelWaiting(), 0);
                newOrder.setIsBottomLabelWaiting(afterWaiting);
                currentType = oldOrder.getBottomLabel();
                currentStatus = oldOrder.getBottomLabelStatus();
                break;
            case STICKER:
                beforeWaiting = defaultInt(oldOrder.getIsStickerWaiting(), 0);
                newOrder.setIsStickerWaiting(afterWaiting);
                currentType = oldOrder.getSticker();
                currentStatus = oldOrder.getStickerStatus();
                break;
            case PRINTING:
                beforeWaiting = defaultInt(oldOrder.getIsPrintingWaiting(), 0);
                newOrder.setIsPrintingWaiting(afterWaiting);
                currentType = oldOrder.getPrinting();
                currentStatus = oldOrder.getPrintingStatus();
                currentText = oldOrder.getPrintingPatternCode();
                break;
            case INNER_BOX:
                beforeWaiting = defaultInt(oldOrder.getIsInnerBoxWaiting(), 0);
                newOrder.setIsInnerBoxWaiting(afterWaiting);
                currentType = oldOrder.getInnerBox();
                currentStatus = oldOrder.getInnerBoxStatus();
                break;
            case COLOR_BOX:
                beforeWaiting = defaultInt(oldOrder.getIsColorBoxWaiting(), 0);
                newOrder.setIsColorBoxWaiting(afterWaiting);
                currentType = oldOrder.getColorBox();
                currentStatus = oldOrder.getColorBoxStatus();
                break;
            case CARTON:
                beforeWaiting = defaultInt(oldOrder.getIsCartonWaiting(), 0);
                newOrder.setIsCartonWaiting(afterWaiting);
                currentType = null;
                currentStatus = oldOrder.getCartonStatus();
                break;
            default:
                throw new BusinessException(400, "不支持的追踪模块类型");
        }

        if (Objects.equals(beforeWaiting, afterWaiting) && !hasText(dto.getRemarkContent())) {
            return;
        }

        newOrder.setUpdateTime(LocalDateTime.now());
        newOrder.setUpdateBy(currentUser);

        orderTrackingMapper.updateById(newOrder);

        saveWaitingChangeLogIfNeeded(oldOrder.getId(), moduleEnum.getCode(), beforeWaiting, afterWaiting, currentType, currentStatus, currentText, dto.getRemarkContent(), currentUser);
    }

    @Override
    public OrderTrackingDetailVO detail(String id) {
        OrderTracking order = getOrderOrThrow(id);

        OrderTrackingDetailVO vo = new OrderTrackingDetailVO();
        vo.setOrder(order);

        Map<String, List<OrderTrackingLogVO>> recentLogs = new LinkedHashMap<>();
        // 详情首屏只携带整单日志，其他模块日志由 /order-tracking/logs 按需查询。
        recentLogs.put(
                TrackModuleEnum.ORDER.getCode(),
                getRecentModuleLogs(id, TrackModuleEnum.ORDER.getCode(), 3));

        vo.setRecentLogs(recentLogs);
        return vo;
    }

    @Override
    public List<OrderTrackingLogVO> getModuleLogs(String orderId, String moduleType) {
        if (!hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }
        if (!hasText(moduleType)) {
            throw new BusinessException("模块类型不能为空");
        }
        if (TrackModuleEnum.getByCode(moduleType) == null) {
            throw new BusinessException("不支持的追踪模块类型");
        }

        // 数据权限校验：不能通过日志接口读取他人订单。
        getOrderOrThrow(orderId);

        List<OrderTrackingLog> logs = orderTrackingLogMapper.selectList(
                new QueryWrapper<OrderTrackingLog>()
                        .eq("order_id", orderId)
                        .eq("module_type", moduleType)
                        .orderByAsc("sort_no")
                        .orderByAsc("create_time")
                        .orderByAsc("id"));

        List<OrderTrackingLogVO> result = logs.stream()
                .map(log -> toLogVO(log, true))
                .collect(Collectors.toList());
        fillLogCreatorDisplayNames(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        OrderTracking order = getOrderOrThrow(id);
        assertCanEdit(order);

        order.setIsDeleted(1);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(getCurrentUserId());

        orderTrackingMapper.updateById(order);
    }

    private void validateCreate(OrderTrackingSaveDTO dto) {
        if (!hasText(dto.getOrderNo())) {
            throw new BusinessException("订单编号不能为空");
        }
        if (!hasText(dto.getCustomerOrderNo())) {
            throw new BusinessException("客户订单编号不能为空");
        }
        if (!hasText(dto.getCustomerContact())) {
            throw new BusinessException("客户对接人不能为空");
        }
        if (dto.getPlannedDeliveryDate() == null) {
            throw new BusinessException("计划交付日期不能为空");
        }
    }

    private OrderTracking getOrderOrThrow(String id) {
        OrderTracking order = orderTrackingMapper.selectById(id);
        if (order == null || Objects.equals(order.getIsDeleted(), 1)) {
            throw new BusinessException("订单不存在");
        }

        String currentUser = getCurrentUserId();
        if (!Objects.equals(order.getCreateBy(), currentUser)) {
            throw new BusinessException("订单不存在或无权限访问");
        }

        return order;
    }

    private void assertCanEdit(OrderTracking order) {
        if (Objects.equals(order.getIsFinished(), 1)) {
            throw new BusinessException("订单已完成，禁止编辑");
        }
    }

    private void assertTrackTypeNotChangedAfterCreate(OrderTracking oldOrder, OrderTracking newOrder) {
        if (!Objects.equals(oldOrder.getSticker(), newOrder.getSticker())) {
            throw new BusinessException(400, "不干胶类型第一次保存后不允许修改");
        }

        if (!Objects.equals(oldOrder.getPrinting(), newOrder.getPrinting())) {
            throw new BusinessException(400, "花纸类型第一次保存后不允许修改");
        }

        if (!Objects.equals(oldOrder.getInnerBox(), newOrder.getInnerBox())) {
            throw new BusinessException(400, "黄盒是否印刷第一次保存后不允许修改");
        }

        if (!Objects.equals(oldOrder.getColorBox(), newOrder.getColorBox())) {
            throw new BusinessException(400, "彩盒类型第一次保存后不允许修改");
        }
    }

    private String normalizeOrderNo(String orderNo) {
        String value = orderNo == null ? "" : orderNo.trim();
        if (!value.startsWith("CT-")) {
            value = "CT-" + value;
        }
        return value;
    }

    /**
     * 根据类型自动处理默认状态。
     * <p>
     * 结构优化点：
     * 1. 不再把状态码写死在前端；
     * 2. 状态是否合法由 t_order_tracking_option 决定；
     * 3. 类型变化后，如果旧状态不属于新类型，自动置为新类型的第一个状态；
     * 4. 类型没有状态流程时，状态自动置空，避免列表页出现 2300、3300 这类脏数据。
     */
    private void fillDefaultStatus(OrderTracking order) {
        order.setBottomLabel(defaultInt(order.getBottomLabel(), 0));
        order.setBottomLabelStatus(resolveStatus(TrackModuleEnum.BOTTOM_LABEL.getCode(), order.getBottomLabel(), order.getBottomLabelStatus()));

        order.setSticker(defaultInt(order.getSticker(), 0));
        order.setStickerStatus(resolveStatus(TrackModuleEnum.STICKER.getCode(), order.getSticker(), order.getStickerStatus()));

        order.setPrinting(defaultInt(order.getPrinting(), 0));
        order.setPrintingStatus(resolveStatus(TrackModuleEnum.PRINTING.getCode(), order.getPrinting(), order.getPrintingStatus()));
        if (order.getPrintingStatus() != null) {
            order.setPrintingPatternCode(null);
        }
        if (order.getPrinting() == null || Objects.equals(order.getPrinting(), 0) || Objects.equals(order.getPrinting(), 10)) {
            order.setPrintingPatternCode(null);
        }

        order.setInnerBox(defaultInt(order.getInnerBox(), 0));
        order.setInnerBoxStatus(resolveStatus(TrackModuleEnum.INNER_BOX.getCode(), order.getInnerBox(), order.getInnerBoxStatus()));

        order.setColorBox(defaultInt(order.getColorBox(), 0));
        order.setColorBoxStatus(resolveStatus(TrackModuleEnum.COLOR_BOX.getCode(), order.getColorBox(), order.getColorBoxStatus()));

        order.setAntiCutBoard(defaultInt(order.getAntiCutBoard(), 0));

        Integer cartonDefault = optionTranslateService.getFirstStatusValue(TrackModuleEnum.CARTON.getCode(), null);
        if (cartonDefault == null) {
            cartonDefault = 1100;
        }
        if (order.getCartonStatus() == null || !optionTranslateService.isValidStatusForParent(TrackModuleEnum.CARTON.getCode(), null, order.getCartonStatus())) {
            order.setCartonStatus(cartonDefault);
        }
    }

    private Integer resolveStatus(String moduleType, Integer parentValue, Integer currentStatus) {
        if (parentValue == null) {
            return null;
        }

        Integer firstStatus = optionTranslateService.getFirstStatusValue(moduleType, parentValue);
        if (firstStatus == null) {
            return null;
        }

        if (currentStatus == null) {
            return firstStatus;
        }

        if (optionTranslateService.isValidStatusForParent(moduleType, parentValue, currentStatus)) {
            return currentStatus;
        }

        return firstStatus;
    }

    /**
     * 等待回复字段默认值。
     */
    private void fillDefaultWaiting(OrderTracking order) {
        order.setIsBottomLabelWaiting(defaultInt(order.getIsBottomLabelWaiting(), 0));
        order.setIsStickerWaiting(defaultInt(order.getIsStickerWaiting(), 0));
        order.setIsPrintingWaiting(defaultInt(order.getIsPrintingWaiting(), 0));
        order.setIsInnerBoxWaiting(defaultInt(order.getIsInnerBoxWaiting(), 0));
        order.setIsColorBoxWaiting(defaultInt(order.getIsColorBoxWaiting(), 0));
        order.setIsCartonWaiting(defaultInt(order.getIsCartonWaiting(), 0));
    }

    private void saveCreateInitialLogsIfNeeded(OrderTracking order, List<OrderTrackingModuleUpdateDTO> moduleUpdates, String currentUser) {
        Map<String, OrderTrackingModuleUpdateDTO> updateMap = buildModuleUpdateMap(moduleUpdates);

        saveCreateInitialLogForModule(order, TrackModuleEnum.ORDER.getCode(), null, null, null, null, null, null, updateMap.get(TrackModuleEnum.ORDER.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.BOTTOM_LABEL.getCode(), null, order.getBottomLabel(), null, order.getBottomLabelStatus(), null, null, updateMap.get(TrackModuleEnum.BOTTOM_LABEL.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.STICKER.getCode(), null, order.getSticker(), null, order.getStickerStatus(), null, null, updateMap.get(TrackModuleEnum.STICKER.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.PRINTING.getCode(), null, order.getPrinting(), null, order.getPrintingStatus(), null, order.getPrintingPatternCode(), updateMap.get(TrackModuleEnum.PRINTING.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.INNER_BOX.getCode(), null, order.getInnerBox(), null, order.getInnerBoxStatus(), null, null, updateMap.get(TrackModuleEnum.INNER_BOX.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.COLOR_BOX.getCode(), null, order.getColorBox(), null, order.getColorBoxStatus(), null, null, updateMap.get(TrackModuleEnum.COLOR_BOX.getCode()), currentUser);

        saveCreateInitialLogForModule(order, TrackModuleEnum.CARTON.getCode(), null, null, null, order.getCartonStatus(), null, null, updateMap.get(TrackModuleEnum.CARTON.getCode()), currentUser);
    }

    private void saveCreateInitialLogForModule(OrderTracking order, String moduleType, Integer beforeType, Integer afterType, Integer beforeStatus, Integer afterStatus, String beforeText, String afterText, OrderTrackingModuleUpdateDTO updateDTO, String currentUser) {
        String remark = updateDTO == null ? null : updateDTO.getRemarkContent();
        List<OrderTrackingLogImageDTO> images = updateDTO == null ? null : updateDTO.getImages();

        boolean hasRemark = hasText(remark);
        boolean hasImages = images != null && !images.isEmpty();
        boolean waiting = isModuleWaiting(order, moduleType);

        if (!hasRemark && !hasImages && !waiting) {
            return;
        }

        String finalRemark = buildRemarkWithWaiting(remark, waiting);

        saveLog(order.getId(), moduleType, beforeType, afterType, beforeStatus, afterStatus, beforeText, afterText, finalRemark, images, currentUser);
    }

    private void saveModuleLogIfNeeded(String orderId, String moduleType, Integer beforeType, Integer afterType, Integer beforeStatus, Integer afterStatus, String beforeText, String afterText, OrderTrackingModuleUpdateDTO updateDTO, String currentUser) {
        String remark = updateDTO == null ? null : updateDTO.getRemarkContent();
        List<OrderTrackingLogImageDTO> images = updateDTO == null ? null : updateDTO.getImages();

        boolean typeChanged = !Objects.equals(beforeType, afterType);
        boolean statusChanged = !Objects.equals(beforeStatus, afterStatus);
        boolean textChanged = !Objects.equals(cleanText(beforeText), cleanText(afterText));
        boolean hasRemark = hasText(remark);
        boolean hasImages = images != null && !images.isEmpty();

        if (!typeChanged && !statusChanged && !textChanged && !hasRemark && !hasImages) {
            return;
        }

        saveLog(orderId, moduleType, beforeType, afterType, beforeStatus, afterStatus, beforeText, afterText, remark, images, currentUser);
    }

    private void saveWaitingChangeLogIfNeeded(String orderId, String moduleType, Integer beforeWaiting, Integer afterWaiting, Integer currentType, Integer currentStatus, String currentText, String remark, String currentUser) {
        Integer before = defaultInt(beforeWaiting, 0);
        Integer after = defaultInt(afterWaiting, 0);

        if (Objects.equals(before, after) && !hasText(remark)) {
            return;
        }

        String actionText;

        if (!Objects.equals(before, after)) {
            actionText = Objects.equals(after, 1) ? "开启等待回复" : "取消等待回复";
        } else {
            actionText = "等待回复备注";
        }

        String finalRemark;
        if (hasText(remark)) {
            finalRemark = actionText + "：" + remark.trim();
        } else {
            finalRemark = actionText;
        }

        saveLog(orderId, moduleType, currentType, currentType, currentStatus, currentStatus, currentText, currentText, finalRemark, null, currentUser);
    }

    private void saveLog(String orderId, String moduleType, Integer beforeType, Integer afterType, Integer beforeStatus, Integer afterStatus, String beforeText, String afterText, String remark, List<OrderTrackingLogImageDTO> images, String currentUser) {
        OrderTrackingLog log = new OrderTrackingLog();
        log.setOrderId(orderId);
        log.setModuleType(moduleType);
        log.setBeforeType(beforeType);
        log.setAfterType(afterType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setBeforeText(beforeText);
        log.setAfterText(afterText);
        log.setRemarkContent(remark);
        log.setSortNo(nextLogSortNo(orderId, moduleType));
        log.setCreateTime(LocalDateTime.now());
        log.setCreateBy(currentUser);

        orderTrackingLogMapper.insert(log);

        if (images == null || images.isEmpty()) {
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            OrderTrackingLogImageDTO imageDTO = images.get(i);
            if (imageDTO == null || !hasText(imageDTO.getImageUrl())) {
                continue;
            }

            OrderTrackingLogImage image = new OrderTrackingLogImage();
            image.setLogId(log.getId());
            image.setOrderId(orderId);
            image.setModuleType(moduleType);
            image.setImageUrl(imageDTO.getImageUrl());
            image.setImageKey(imageDTO.getImageKey());
            image.setOriginalName(imageDTO.getOriginalName());
            image.setFileSize(imageDTO.getFileSize());
            image.setContentType(imageDTO.getContentType());
            image.setSortNo(imageDTO.getSortNo() == null ? i + 1 : imageDTO.getSortNo());
            image.setCreateTime(LocalDateTime.now());
            image.setCreateBy(currentUser);

            orderTrackingLogImageMapper.insert(image);
        }
    }

    /**
     * 生成同一订单、同一模块下的日志排序号。
     * <p>
     * 说明：
     * 1. 旧数据通过 SQL 按 create_time/id 回填 sort_no；
     * 2. 新日志保存时在当前模块最大 sort_no 基础上递增；
     * 3. 查询时按 sort_no + create_time + id 排序，避免同一秒多条日志顺序不稳定。
     */
    private Long nextLogSortNo(String orderId, String moduleType) {
        OrderTrackingLog lastLog = orderTrackingLogMapper.selectOne(new QueryWrapper<OrderTrackingLog>().eq("order_id", orderId).eq("module_type", moduleType).orderByDesc("sort_no").orderByDesc("create_time").orderByDesc("id").last("LIMIT 1"));

        if (lastLog == null || lastLog.getSortNo() == null) {
            return 1L;
        }

        return lastLog.getSortNo() + 1L;
    }

    private Map<String, OrderTrackingModuleUpdateDTO> buildModuleUpdateMap(List<OrderTrackingModuleUpdateDTO> updates) {
        Map<String, OrderTrackingModuleUpdateDTO> map = new HashMap<>();

        if (updates == null || updates.isEmpty()) {
            return map;
        }

        for (OrderTrackingModuleUpdateDTO update : updates) {
            if (update == null || !hasText(update.getModuleType())) {
                continue;
            }
            map.put(update.getModuleType(), update);
        }

        return map;
    }

    private boolean isModuleWaiting(OrderTracking order, String moduleType) {
        if (TrackModuleEnum.BOTTOM_LABEL.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsBottomLabelWaiting(), 0), 1);
        }

        if (TrackModuleEnum.STICKER.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsStickerWaiting(), 0), 1);
        }

        if (TrackModuleEnum.PRINTING.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsPrintingWaiting(), 0), 1);
        }

        if (TrackModuleEnum.INNER_BOX.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsInnerBoxWaiting(), 0), 1);
        }

        if (TrackModuleEnum.COLOR_BOX.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsColorBoxWaiting(), 0), 1);
        }

        if (TrackModuleEnum.CARTON.getCode().equals(moduleType)) {
            return Objects.equals(defaultInt(order.getIsCartonWaiting(), 0), 1);
        }

        return false;
    }

    private String buildRemarkWithWaiting(String remark, boolean waiting) {
        StringBuilder builder = new StringBuilder();

        if (waiting) {
            builder.append("开启等待回复");
        }

        if (hasText(remark)) {
            if (builder.length() > 0) {
                builder.append("：");
            }
            builder.append(remark.trim());
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private List<OrderTrackingLogVO> getRecentModuleLogs(String orderId, String moduleType, int limit) {
        List<OrderTrackingLog> logs = orderTrackingLogMapper.selectList(new QueryWrapper<OrderTrackingLog>().eq("order_id", orderId).eq("module_type", moduleType).orderByDesc("sort_no").orderByDesc("create_time").orderByDesc("id").last("LIMIT " + limit));

        List<OrderTrackingLogVO> result = logs.stream()
                .map(log -> toLogVO(log, false))
                .collect(Collectors.toList());
        fillLogCreatorDisplayNames(result);
        return result;
    }

    private void fillOperatorDisplayNames(List<OrderTrackingListVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<String> userIds = new LinkedHashSet<>();
        for (OrderTrackingListVO record : records) {
            addUserId(userIds, record.getCreateBy());
            addUserId(userIds, record.getUpdateBy());
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<String, String> displayNameMap = loadUsernameMap(userIds);

        for (OrderTrackingListVO record : records) {
            record.setCreateBy(displayNameMap.getOrDefault(record.getCreateBy(), record.getCreateBy()));
            record.setUpdateBy(displayNameMap.getOrDefault(record.getUpdateBy(), record.getUpdateBy()));
        }
    }

    private void fillLogCreatorDisplayNames(List<OrderTrackingLogVO> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }

        Set<String> userIds = new LinkedHashSet<>();
        for (OrderTrackingLogVO log : logs) {
            addUserId(userIds, log.getCreateBy());
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<String, String> displayNameMap = loadUsernameMap(userIds);
        for (OrderTrackingLogVO log : logs) {
            log.setCreateBy(displayNameMap.getOrDefault(log.getCreateBy(), log.getCreateBy()));
        }
    }

    private Map<String, String> loadUsernameMap(Set<String> userIds) {
        List<SysUser> users = sysUserService.listByIds(userIds);
        Map<String, String> displayNameMap = new HashMap<>();
        for (SysUser user : users) {
            if (user != null && hasText(user.getId())) {
                displayNameMap.put(user.getId(), resolveUserDisplayName(user));
            }
        }
        return displayNameMap;
    }

    private void addUserId(Set<String> userIds, String userId) {
        if (hasText(userId)) {
            userIds.add(userId);
        }
    }

    private String resolveUserDisplayName(SysUser user) {
        if (hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return user.getId();
    }

    private OrderTrackingLogVO toLogVO(OrderTrackingLog log, boolean includeImages) {
        OrderTrackingLogVO vo = new OrderTrackingLogVO();
        BeanUtils.copyProperties(log, vo);

        Long imageCount = orderTrackingLogImageMapper.selectCount(new QueryWrapper<OrderTrackingLogImage>().eq("log_id", log.getId()));
        vo.setImageCount(imageCount == null ? 0L : imageCount);

        if (includeImages) {
            List<OrderTrackingLogImage> images = orderTrackingLogImageMapper.selectList(new QueryWrapper<OrderTrackingLogImage>().eq("log_id", log.getId()).orderByAsc("sort_no").orderByAsc("create_time").orderByAsc("id"));

            List<OrderTrackingLogImageVO> imageVOS = images.stream().map(image -> {
                OrderTrackingLogImageVO imageVO = new OrderTrackingLogImageVO();
                BeanUtils.copyProperties(image, imageVO);
                return imageVO;
            }).collect(Collectors.toList());

            vo.setImages(imageVOS);
        }

        return vo;
    }

    private String getCurrentUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }
}
