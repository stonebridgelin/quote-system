package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingStatusOptionUpdateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingStatusOptionVO;
import com.stonebridge.quotesystem.business.enums.TrackModuleEnum;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionService;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderTrackingOptionServiceImpl implements IOrderTrackingOptionService {

    private static final String OPTION_TYPE_STATUS = "STATUS";

    private final OrderTrackingOptionMapper orderTrackingOptionMapper;
    private final IOrderTrackingOptionTranslateService optionTranslateService;

    @Override
    public Map<String, List<OrderTrackingOptionVO>> getOptionMap() {
        List<OrderTrackingOption> list = loadEnabledOptions();

        List<OrderTrackingOptionVO> voList = list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, List<OrderTrackingOptionVO>> result = voList.stream()
                .collect(Collectors.groupingBy(
                        item -> buildKey(item.getModuleType(), item.getOptionType(), item.getParentValue()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 结构优化：额外提供 MODULE_STATUS_ALL 兜底分组。
        // 列表页已改为后端 VO 翻译，但表单或其他动态页面仍可使用 ALL 做前端兜底。
        Map<String, List<OrderTrackingOptionVO>> statusAllMap = voList.stream()
                .filter(item -> "STATUS".equals(item.getOptionType()))
                .collect(Collectors.groupingBy(
                        item -> item.getModuleType() + "_STATUS_ALL",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        statusAllMap.forEach((key, value) -> result.put(key, new ArrayList<>(value)));

        return result;
    }

    @Override
    public List<OrderTrackingStatusOptionVO> getStatusOptions() {
        List<OrderTrackingOption> options = loadEnabledOptions();
        Map<String, String> typeLabelMap = buildTypeLabelMap(options);

        return options.stream()
                .filter(item -> OPTION_TYPE_STATUS.equals(item.getOptionType()))
                .map(item -> toStatusVO(item, typeLabelMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTrackingStatusOptionVO updateStatusOption(
            String id,
            OrderTrackingStatusOptionUpdateDTO dto) {
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessException(400, "状态配置ID不能为空");
        }

        OrderTrackingOption option = orderTrackingOptionMapper.selectById(id);
        if (option == null
                || Integer.valueOf(1).equals(option.getIsDeleted())
                || !Integer.valueOf(1).equals(option.getIsEnabled())) {
            throw new BusinessException(404, "状态配置不存在");
        }
        if (!OPTION_TYPE_STATUS.equals(option.getOptionType())) {
            throw new BusinessException(400, "只能修改状态配置");
        }

        assertStatusCodeUnique(option, dto.getOptionValue());

        option.setOptionValue(dto.getOptionValue());
        option.setOptionLabel(dto.getOptionLabel().trim());
        option.setSortNo(dto.getSortNo());
        option.setTagType(dto.getTagType().trim());
        option.setRemark(trimToNull(dto.getRemark()));
        option.setUpdateTime(LocalDateTime.now());
        option.setUpdateBy(SecurityUtil.getCurrentUserId());

        if (orderTrackingOptionMapper.updateById(option) != 1) {
            throw new BusinessException("状态配置更新失败");
        }

        refreshTranslationCacheAfterCommit();

        Map<String, String> typeLabelMap =
                buildTypeLabelMap(loadEnabledOptions());
        return toStatusVO(option, typeLabelMap);
    }

    private List<OrderTrackingOption> loadEnabledOptions() {
        return orderTrackingOptionMapper.selectList(
                new QueryWrapper<OrderTrackingOption>()
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
                        .orderByAsc("module_type")
                        .orderByAsc("option_type")
                        .orderByAsc("parent_value")
                        .orderByAsc("sort_no")
                        .orderByAsc("option_value"));
    }

    private void assertStatusCodeUnique(
            OrderTrackingOption current,
            Integer optionValue) {
        QueryWrapper<OrderTrackingOption> wrapper =
                new QueryWrapper<OrderTrackingOption>()
                        .eq("module_type", current.getModuleType())
                        .eq("option_type", OPTION_TYPE_STATUS)
                        .eq("option_value", optionValue)
                        .eq("is_deleted", 0)
                        .ne("id", current.getId());

        if (current.getParentValue() == null) {
            wrapper.isNull("parent_value");
        } else {
            wrapper.eq("parent_value", current.getParentValue());
        }

        Long count = orderTrackingOptionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(
                    400,
                    "同一模块和类型下已存在状态码：" + optionValue);
        }
    }

    private Map<String, String> buildTypeLabelMap(
            List<OrderTrackingOption> options) {
        Map<String, String> result = new HashMap<>();
        for (OrderTrackingOption option : options) {
            if ("TYPE".equals(option.getOptionType())) {
                result.put(
                        typeKey(option.getModuleType(), option.getOptionValue()),
                        option.getOptionLabel());
            }
        }
        return result;
    }

    private OrderTrackingStatusOptionVO toStatusVO(
            OrderTrackingOption option,
            Map<String, String> typeLabelMap) {
        OrderTrackingStatusOptionVO vo =
                new OrderTrackingStatusOptionVO();
        BeanUtils.copyProperties(option, vo);

        TrackModuleEnum module =
                TrackModuleEnum.getByCode(option.getModuleType());
        vo.setModuleLabel(
                module == null ? option.getModuleType() : module.getLabel());

        if (option.getParentValue() == null) {
            vo.setParentLabel("无上级类型");
        } else {
            vo.setParentLabel(typeLabelMap.getOrDefault(
                    typeKey(
                            option.getModuleType(),
                            option.getParentValue()),
                    String.valueOf(option.getParentValue())));
        }
        return vo;
    }

    private void refreshTranslationCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            optionTranslateService.refreshCache();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        optionTranslateService.refreshCache();
                    }
                });
    }

    private OrderTrackingOptionVO toVO(OrderTrackingOption option) {
        OrderTrackingOptionVO vo = new OrderTrackingOptionVO();
        BeanUtils.copyProperties(option, vo);
        vo.setValue(option.getOptionValue());
        vo.setLabel(option.getOptionLabel());
        return vo;
    }

    private String buildKey(String moduleType, String optionType, Integer parentValue) {
        if ("STATUS".equals(optionType) && parentValue != null) {
            return moduleType + "_" + optionType + "_" + parentValue;
        }
        return moduleType + "_" + optionType;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String typeKey(String moduleType, Integer optionValue) {
        return moduleType + "|" + optionValue;
    }
}
