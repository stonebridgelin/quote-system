package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTracking;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingOptionTranslateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionTranslateVO;
import com.stonebridge.quotesystem.business.enums.TrackModuleEnum;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单追踪配置翻译服务。
 *
 * 这是本次结构优化的核心：
 * 1. 列表页展示字段由后端统一翻译，不再依赖前端 optionMap 兜底；
 * 2. STATUS 支持两级查找：先按 parentValue 精确匹配，再按模块全量状态兜底；
 * 3. 所有配置来自 t_order_tracking_option；
 * 4. 使用本地内存缓存，避免列表页频繁查询配置表。
 */
@Service
@RequiredArgsConstructor
public class OrderTrackingOptionTranslateServiceImpl implements IOrderTrackingOptionTranslateService {

    private static final String OPTION_TYPE_TYPE = "TYPE";
    private static final String OPTION_TYPE_STATUS = "STATUS";

    private final OrderTrackingOptionMapper orderTrackingOptionMapper;

    private volatile OptionCache optionCache;

    @Override
    public OrderTrackingListVO toListVO(OrderTracking order) {
        OrderTrackingListVO vo = new OrderTrackingListVO();
        BeanUtils.copyProperties(order, vo);

        fillModuleDisplay(
                vo,
                TrackModuleEnum.BOTTOM_LABEL.getCode(),
                order.getBottomLabel(),
                order.getBottomLabelStatus(),
                "bottomLabel"
        );

        fillModuleDisplay(
                vo,
                TrackModuleEnum.STICKER.getCode(),
                order.getSticker(),
                order.getStickerStatus(),
                "sticker"
        );

        fillModuleDisplay(
                vo,
                TrackModuleEnum.PRINTING.getCode(),
                order.getPrinting(),
                order.getPrintingStatus(),
                "printing"
        );

        fillModuleDisplay(
                vo,
                TrackModuleEnum.INNER_BOX.getCode(),
                order.getInnerBox(),
                order.getInnerBoxStatus(),
                "innerBox"
        );

        fillModuleDisplay(
                vo,
                TrackModuleEnum.COLOR_BOX.getCode(),
                order.getColorBox(),
                order.getColorBoxStatus(),
                "colorBox"
        );

        OrderTrackingOptionTranslateVO cartonStatus = translate(
                TrackModuleEnum.CARTON.getCode(),
                OPTION_TYPE_STATUS,
                null,
                order.getCartonStatus()
        );
        vo.setCartonStatusText(cartonStatus.getLabel());
        vo.setCartonDisplayText(cartonStatus.getLabel());
        vo.setCartonTagType(defaultTag(cartonStatus.getTagType()));

        OrderTrackingOptionTranslateVO antiCutBoard = translate(
                "ANTI_CUT_BOARD",
                OPTION_TYPE_TYPE,
                null,
                order.getAntiCutBoard()
        );
        vo.setAntiCutBoardText(antiCutBoard.getLabel());
        vo.setAntiCutBoardTagType(defaultTag(antiCutBoard.getTagType()));

        return vo;
    }

    @Override
    public OrderTrackingOptionTranslateVO translate(String moduleType, String optionType, Integer parentValue, Integer value) {
        OptionCache cache = getCache();

        String normalizedModule = normalize(moduleType);
        String normalizedOptionType = normalize(optionType);

        OrderTrackingOptionTranslateVO vo = new OrderTrackingOptionTranslateVO();
        vo.setModuleType(normalizedModule);
        vo.setOptionType(normalizedOptionType);
        vo.setParentValue(parentValue);
        vo.setValue(value);

        if (value == null) {
            vo.setLabel("-");
            vo.setTagType("info");
            vo.setMatched(false);
            vo.setMatchedBy("EMPTY");
            vo.setValidForParent(false);
            return vo;
        }

        if (OPTION_TYPE_TYPE.equals(normalizedOptionType)) {
            OptionItem item = cache.typeMap.get(typeKey(normalizedModule, value));
            if (item != null) {
                fillMatched(vo, item, "TYPE", true);
            } else {
                fillUnmatched(vo, "未知类型(" + value + ")");
            }
            return vo;
        }

        if (OPTION_TYPE_STATUS.equals(normalizedOptionType)) {
            OptionItem exact = cache.statusParentMap.get(statusParentKey(normalizedModule, parentValue, value));
            if (exact != null) {
                fillMatched(vo, exact, "PARENT", true);
                return vo;
            }

            OptionItem any = cache.statusAnyMap.get(statusAnyKey(normalizedModule, value));
            if (any != null) {
                fillMatched(vo, any, "ALL", false);
                return vo;
            }

            fillUnmatched(vo, "未知状态(" + value + ")");
            return vo;
        }

        fillUnmatched(vo, String.valueOf(value));
        return vo;
    }

    @Override
    public List<OrderTrackingOptionTranslateVO> translateBatch(List<OrderTrackingOptionTranslateDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        return requests.stream()
                .filter(Objects::nonNull)
                .map(item -> translate(item.getModuleType(), item.getOptionType(), item.getParentValue(), item.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValidStatusForParent(String moduleType, Integer parentValue, Integer statusValue) {
        if (statusValue == null) {
            return false;
        }
        return getCache().statusParentMap.containsKey(statusParentKey(normalize(moduleType), parentValue, statusValue));
    }

    @Override
    public Integer getFirstStatusValue(String moduleType, Integer parentValue) {
        List<OptionItem> list = getCache().statusListByParent.get(statusListKey(normalize(moduleType), parentValue));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0).value;
    }

    @Override
    public void refreshCache() {
        synchronized (this) {
            this.optionCache = loadCache();
        }
    }

    private OptionCache getCache() {
        OptionCache cache = optionCache;
        if (cache == null) {
            synchronized (this) {
                cache = optionCache;
                if (cache == null) {
                    cache = loadCache();
                    optionCache = cache;
                }
            }
        }
        return cache;
    }

    private OptionCache loadCache() {
        List<OrderTrackingOption> list = orderTrackingOptionMapper.selectList(
                new QueryWrapper<OrderTrackingOption>()
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
                        .orderByAsc("module_type")
                        .orderByAsc("option_type")
                        .orderByAsc("parent_value")
                        .orderByAsc("sort_no")
        );

        OptionCache cache = new OptionCache();

        for (OrderTrackingOption option : list) {
            if (option == null || option.getOptionValue() == null) {
                continue;
            }

            String moduleType = normalize(option.getModuleType());
            String optionType = normalize(option.getOptionType());

            OptionItem item = new OptionItem();
            item.moduleType = moduleType;
            item.optionType = optionType;
            item.parentValue = option.getParentValue();
            item.value = option.getOptionValue();
            item.label = option.getOptionLabel();
            item.tagType = defaultTag(option.getTagType());
            item.sortNo = option.getSortNo() == null ? 0 : option.getSortNo();

            if (OPTION_TYPE_TYPE.equals(optionType)) {
                cache.typeMap.put(typeKey(moduleType, item.value), item);
            }

            if (OPTION_TYPE_STATUS.equals(optionType)) {
                if (item.parentValue != null) {
                    cache.statusParentMap.put(statusParentKey(moduleType, item.parentValue, item.value), item);
                    cache.statusListByParent.computeIfAbsent(statusListKey(moduleType, item.parentValue), k -> new ArrayList<>()).add(item);
                } else {
                    cache.statusParentMap.put(statusParentKey(moduleType, null, item.value), item);
                    cache.statusListByParent.computeIfAbsent(statusListKey(moduleType, null), k -> new ArrayList<>()).add(item);
                }

                // 模块级兜底映射。若同一模块内状态码重复，保留排序靠前的第一条。
                cache.statusAnyMap.putIfAbsent(statusAnyKey(moduleType, item.value), item);
            }
        }

        for (List<OptionItem> items : cache.statusListByParent.values()) {
            items.sort((a, b) -> Integer.compare(a.sortNo, b.sortNo));
        }

        return cache;
    }

    private void fillModuleDisplay(OrderTrackingListVO vo,
                                   String moduleType,
                                   Integer typeValue,
                                   Integer statusValue,
                                   String prefix) {
        OrderTrackingOptionTranslateVO type = translate(moduleType, OPTION_TYPE_TYPE, null, typeValue);
        OrderTrackingOptionTranslateVO status = translate(moduleType, OPTION_TYPE_STATUS, typeValue, statusValue);

        String typeText = type.getLabel();
        String statusText = "-".equals(status.getLabel()) ? null : status.getLabel();
        String displayText = hasText(statusText) ? statusText : typeText;
        String tagType = hasText(statusText) ? status.getTagType() : type.getTagType();

        if ("bottomLabel".equals(prefix)) {
            vo.setBottomLabelTypeText(typeText);
            vo.setBottomLabelStatusText(statusText);
            vo.setBottomLabelDisplayText(displayText);
            vo.setBottomLabelTagType(defaultTag(tagType));
        } else if ("sticker".equals(prefix)) {
            vo.setStickerTypeText(typeText);
            vo.setStickerStatusText(statusText);
            vo.setStickerDisplayText(displayText);
            vo.setStickerTagType(defaultTag(tagType));
        } else if ("printing".equals(prefix)) {
            vo.setPrintingTypeText(typeText);
            vo.setPrintingStatusText(statusText);
            vo.setPrintingDisplayText(displayText);
            vo.setPrintingTagType(defaultTag(tagType));
        } else if ("innerBox".equals(prefix)) {
            vo.setInnerBoxTypeText(typeText);
            vo.setInnerBoxStatusText(statusText);
            vo.setInnerBoxDisplayText(displayText);
            vo.setInnerBoxTagType(defaultTag(tagType));
        } else if ("colorBox".equals(prefix)) {
            vo.setColorBoxTypeText(typeText);
            vo.setColorBoxStatusText(statusText);
            vo.setColorBoxDisplayText(displayText);
            vo.setColorBoxTagType(defaultTag(tagType));
        }
    }

    private void fillMatched(OrderTrackingOptionTranslateVO vo, OptionItem item, String matchedBy, boolean validForParent) {
        vo.setLabel(item.label);
        vo.setTagType(defaultTag(item.tagType));
        vo.setMatched(true);
        vo.setMatchedBy(matchedBy);
        vo.setValidForParent(validForParent);
    }

    private void fillUnmatched(OrderTrackingOptionTranslateVO vo, String label) {
        vo.setLabel(label);
        vo.setTagType("danger");
        vo.setMatched(false);
        vo.setMatchedBy("NONE");
        vo.setValidForParent(false);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultTag(String tagType) {
        return hasText(tagType) ? tagType : "info";
    }

    private static String typeKey(String moduleType, Integer value) {
        return moduleType + "|TYPE|" + value;
    }

    private static String statusParentKey(String moduleType, Integer parentValue, Integer value) {
        return moduleType + "|STATUS|" + parentValue + "|" + value;
    }

    private static String statusAnyKey(String moduleType, Integer value) {
        return moduleType + "|STATUS|ALL|" + value;
    }

    private static String statusListKey(String moduleType, Integer parentValue) {
        return moduleType + "|STATUS_LIST|" + parentValue;
    }

    private static class OptionCache {
        private final Map<String, OptionItem> typeMap = new HashMap<>();
        private final Map<String, OptionItem> statusParentMap = new HashMap<>();
        private final Map<String, OptionItem> statusAnyMap = new HashMap<>();
        private final Map<String, List<OptionItem>> statusListByParent = new LinkedHashMap<>();
    }

    private static class OptionItem {
        private String moduleType;
        private String optionType;
        private Integer parentValue;
        private Integer value;
        private String label;
        private String tagType;
        private Integer sortNo;
    }
}
