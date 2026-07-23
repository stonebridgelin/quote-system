package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.Customer;
import com.stonebridge.quotesystem.business.entity.SignedOrderDetail;
import com.stonebridge.quotesystem.business.entity.SignedOrderMain;
import com.stonebridge.quotesystem.business.entity.SignedOrderSetItem;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderDetailSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSetItemSaveDTO;
import com.stonebridge.quotesystem.business.entity.vo.CustomerOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SalesmanOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderLineVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderListVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderPageVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderSetItemVO;
import com.stonebridge.quotesystem.business.enums.SignedOrderCurrency;
import com.stonebridge.quotesystem.business.enums.SignedOrderLineType;
import com.stonebridge.quotesystem.business.enums.SignedOrderStatus;
import com.stonebridge.quotesystem.business.mapper.CustomerMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderDetailMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderMainMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderSetItemMapper;
import com.stonebridge.quotesystem.business.service.ISignedOrderService;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignedOrderServiceImpl implements ISignedOrderService {

    private static final int MONEY_SCALE = 2;
    private static final int UNIT_PRICE_SCALE = 4;
    private static final int WEIGHT_SCALE = 3;
    private static final int MAX_DETAIL_COUNT = 500;
    private static final int MAX_SET_ITEM_COUNT = 200;

    private final CustomerMapper customerMapper;
    private final SignedOrderMainMapper signedOrderMainMapper;
    private final SignedOrderDetailMapper signedOrderDetailMapper;
    private final SignedOrderSetItemMapper signedOrderSetItemMapper;
    private final SysUserService sysUserService;

    @Override
    public SignedOrderPageVO page(SignedOrderPageQueryDTO queryDTO) {
        SignedOrderPageQueryDTO query = queryDTO == null ? new SignedOrderPageQueryDTO() : queryDTO;
        validateQueryRange(query);

        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 15L : query.getSize();

        QueryWrapper<SignedOrderMain> pageWrapper = buildQueryWrapper(query);
        pageWrapper.orderByDesc("create_date")
                .orderByDesc("create_time")
                .orderByDesc("id");

        Page<SignedOrderMain> entityPage = signedOrderMainMapper.selectPage(
                new Page<>(current, size), pageWrapper);

        SignedOrderPageVO result = new SignedOrderPageVO();
        result.setCurrent(entityPage.getCurrent());
        result.setSize(entityPage.getSize());
        result.setTotal(entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList()));

        Map<String, BigDecimal> currencyTotals = queryCurrencyTotals(query);
        result.setCnyTotal(currencyTotals.getOrDefault(
                SignedOrderCurrency.CNY.name(), zeroMoney()));
        result.setUsdTotal(currencyTotals.getOrDefault(
                SignedOrderCurrency.USD.name(), zeroMoney()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SignedOrderSaveDTO dto) {
        validateSaveRequest(dto);

        String orderNo = requireText(dto.getOrderNo(), "订单编号不能为空");
        if (signedOrderMainMapper.selectCount(new LambdaQueryWrapper<SignedOrderMain>()
                .eq(SignedOrderMain::getOrderNo, orderNo)) > 0) {
            throw new BusinessException(400, "订单编号已存在");
        }

        Customer customer = findOrCreateCustomer(dto.getCustomerId(), dto.getCustomerName());
        SysUser salesman = requireActiveSalesman(dto.getSalesmanId());
        String currency = normalizeCurrency(dto.getCurrency());
        LocalDateTime now = LocalDateTime.now();
        String orderId = IdWorker.get32UUID();

        PreparedLines prepared = prepareLines(dto.getDetailList(), orderId, now);

        SignedOrderMain main = new SignedOrderMain();
        main.setId(orderId);
        main.setOrderNo(orderNo);
        main.setCustomerId(customer.getId());
        main.setCustomerName(customer.getCustomerName());
        main.setSalesmanId(salesman.getId());
        main.setSalesmanName(resolveSalesmanName(salesman));
        main.setCreateDate(dto.getCreateDate() == null ? LocalDate.now() : dto.getCreateDate());
        main.setCurrency(currency);
        main.setTotalPcs(prepared.totalPcs());
        main.setTotalSets(prepared.totalSets());
        main.setTotalAmount(prepared.totalAmount());
        main.setStatus(SignedOrderStatus.ACTIVE.name());
        main.setRemark(trimToNull(dto.getRemark()));
        main.setCreateTime(now);
        main.setUpdateTime(now);
        main.setCreator(SecurityUtil.getCurrentUsername());

        if (signedOrderMainMapper.insert(main) != 1) {
            throw new BusinessException("保存订单主表失败");
        }
        if (signedOrderDetailMapper.insertBatch(prepared.details()) != prepared.details().size()) {
            throw new BusinessException("保存订单商品明细失败");
        }
        if (!prepared.setItems().isEmpty()
                && signedOrderSetItemMapper.insertBatch(prepared.setItems()) != prepared.setItems().size()) {
            throw new BusinessException("保存套装组成失败");
        }
        return orderId;
    }

    @Override
    public SignedOrderDetailVO detail(String id) {
        String orderId = requireText(id, "订单ID不能为空");
        SignedOrderMain main = signedOrderMainMapper.selectById(orderId);
        if (main == null) {
            throw new BusinessException(404, "订单不存在");
        }

        List<SignedOrderDetail> details = signedOrderDetailMapper.selectList(
                new LambdaQueryWrapper<SignedOrderDetail>()
                        .eq(SignedOrderDetail::getOrderId, orderId)
                        .orderByAsc(SignedOrderDetail::getSortNo)
                        .orderByAsc(SignedOrderDetail::getId));

        List<String> detailIds = details.stream()
                .map(SignedOrderDetail::getId)
                .toList();

        Map<String, List<SignedOrderSetItem>> itemsByDetailId = new HashMap<>();
        if (!detailIds.isEmpty()) {
            List<SignedOrderSetItem> setItems = signedOrderSetItemMapper.selectList(
                    new LambdaQueryWrapper<SignedOrderSetItem>()
                            .in(SignedOrderSetItem::getOrderDetailId, detailIds)
                            .orderByAsc(SignedOrderSetItem::getOrderDetailId)
                            .orderByAsc(SignedOrderSetItem::getSortNo)
                            .orderByAsc(SignedOrderSetItem::getId));
            itemsByDetailId = setItems.stream()
                    .collect(Collectors.groupingBy(
                            SignedOrderSetItem::getOrderDetailId,
                            Collectors.toList()));
        }

        SignedOrderDetailVO result = new SignedOrderDetailVO();
        BeanUtils.copyProperties(main, result);
        Map<String, List<SignedOrderSetItem>> finalItemsByDetailId = itemsByDetailId;
        result.setDetailList(details.stream()
                .map(detail -> toLineVO(
                        detail,
                        finalItemsByDetailId.getOrDefault(detail.getId(), Collections.emptyList())))
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String id, String cancelReason) {
        String orderId = requireText(id, "订单ID不能为空");
        String reason = requireText(cancelReason, "取消原因不能为空");
        if (reason.length() > 500) {
            throw new BusinessException(400, "取消原因不能超过500个字符");
        }

        SignedOrderMain main = signedOrderMainMapper.selectById(orderId);
        if (main == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (SignedOrderStatus.CANCELLED.name().equals(main.getStatus())) {
            return;
        }

        int updated = signedOrderMainMapper.update(null,
                new LambdaUpdateWrapper<SignedOrderMain>()
                        .eq(SignedOrderMain::getId, orderId)
                        .eq(SignedOrderMain::getStatus, SignedOrderStatus.ACTIVE.name())
                        .set(SignedOrderMain::getStatus, SignedOrderStatus.CANCELLED.name())
                        .set(SignedOrderMain::getCancelTime, LocalDateTime.now())
                        .set(SignedOrderMain::getCancelReason, reason)
                        .set(SignedOrderMain::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(409, "订单状态已发生变化，请刷新后重试");
        }
    }

    @Override
    public List<CustomerOptionVO> customerOptions(String keyword, Integer limit) {
        int safeLimit = normalizeOptionLimit(limit);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Customer::getCustomerName, keyword.trim());
        }
        wrapper.orderByAsc(Customer::getCustomerName)
                .last("LIMIT " + safeLimit);
        return customerMapper.selectList(wrapper).stream()
                .map(customer -> new CustomerOptionVO(
                        customer.getId(), customer.getCustomerName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SalesmanOptionVO> salesmanOptions(String keyword, Integer limit) {
        int safeLimit = normalizeOptionLimit(limit);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(condition -> condition
                    .like(SysUser::getUsername, value)
                    .or().like(SysUser::getRealName, value)
                    .or().like(SysUser::getNickname, value));
        }
        wrapper.orderByAsc(SysUser::getRealName)
                .orderByAsc(SysUser::getUsername)
                .last("LIMIT " + safeLimit);

        return sysUserService.list(wrapper).stream()
                .map(user -> new SalesmanOptionVO(
                        user.getId(), user.getUsername(), resolveSalesmanName(user)))
                .collect(Collectors.toList());
    }

    private PreparedLines prepareLines(List<SignedOrderDetailSaveDTO> detailDTOs,
                                       String orderId,
                                       LocalDateTime now) {
        if (detailDTOs.size() > MAX_DETAIL_COUNT) {
            throw new BusinessException(400, "单个订单最多保存" + MAX_DETAIL_COUNT + "条商品明细");
        }

        List<SignedOrderDetail> details = new ArrayList<>(detailDTOs.size());
        List<SignedOrderSetItem> setItems = new ArrayList<>();
        long orderTotalPcs = 0L;
        long orderTotalSets = 0L;
        BigDecimal orderTotalAmount = zeroMoney();

        for (int index = 0; index < detailDTOs.size(); index++) {
            SignedOrderDetailSaveDTO dto = detailDTOs.get(index);
            if (dto == null) {
                throw new BusinessException(400, "第" + (index + 1) + "条商品明细不能为空");
            }

            SignedOrderLineType lineType = normalizeLineType(dto.getLineType(), index);
            BigDecimal unitPrice = normalizeUnitPrice(dto.getUnitPrice(), index);
            SignedOrderDetail detail = new SignedOrderDetail();
            detail.setId(IdWorker.get32UUID());
            detail.setOrderId(orderId);
            detail.setSortNo(index + 1);
            detail.setLineType(lineType.name());
            detail.setUnitPrice(unitPrice);
            detail.setCreateTime(now);

            if (lineType == SignedOrderLineType.SINGLE) {
                prepareSingleLine(dto, detail, index);
            } else {
                prepareSetLine(dto, detail, setItems, now, index);
            }

            orderTotalPcs = safeAdd(orderTotalPcs, detail.getTotalPcs(), "订单总件数过大");
            orderTotalSets = safeAdd(orderTotalSets, detail.getTotalSets(), "订单总套数过大");
            orderTotalAmount = orderTotalAmount.add(detail.getAmount());
            details.add(detail);
        }

        return new PreparedLines(
                details,
                setItems,
                orderTotalPcs,
                orderTotalSets,
                orderTotalAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private void prepareSingleLine(SignedOrderDetailSaveDTO dto,
                                   SignedOrderDetail detail,
                                   int index) {
        String label = "第" + (index + 1) + "条单品";
        detail.setProductCode(requireText(dto.getProductCode(), label + "商品代号不能为空"));
        detail.setWeight(normalizeWeight(dto.getWeight(), label));
        detail.setTotalPcs(requirePositive(dto.getTotalPcs(), label + "总件数必须大于0"));
        detail.setTotalSets(0L);
        detail.setAmount(calculateAmount(detail.getUnitPrice(), detail.getTotalPcs()));

        if (dto.getSetItems() != null && !dto.getSetItems().isEmpty()) {
            throw new BusinessException(400, label + "不能包含套装组成");
        }
    }

    private void prepareSetLine(SignedOrderDetailSaveDTO dto,
                                SignedOrderDetail detail,
                                List<SignedOrderSetItem> allSetItems,
                                LocalDateTime now,
                                int index) {
        String label = "第" + (index + 1) + "条套装";
        List<SignedOrderSetItemSaveDTO> itemDTOs = dto.getSetItems();
        if (itemDTOs == null || itemDTOs.size() < 2) {
            throw new BusinessException(400, label + "至少需要两种组成商品");
        }
        if (itemDTOs.size() > MAX_SET_ITEM_COUNT) {
            throw new BusinessException(400, label + "最多包含" + MAX_SET_ITEM_COUNT + "种商品");
        }

        long totalSets = requirePositive(dto.getTotalSets(), label + "订购套数必须大于0");
        long pcsPerSet = 0L;
        BigDecimal weightPerSet = BigDecimal.ZERO;
        Set<String> productCodes = new HashSet<>();

        for (int itemIndex = 0; itemIndex < itemDTOs.size(); itemIndex++) {
            SignedOrderSetItemSaveDTO itemDTO = itemDTOs.get(itemIndex);
            if (itemDTO == null) {
                throw new BusinessException(400, label + "第" + (itemIndex + 1) + "个组成商品不能为空");
            }

            String productCode = requireText(
                    itemDTO.getProductCode(),
                    label + "第" + (itemIndex + 1) + "个商品代号不能为空");
            String normalizedCode = productCode.toLowerCase(Locale.ROOT);
            if (!productCodes.add(normalizedCode)) {
                throw new BusinessException(400, label + "存在重复商品代号：" + productCode);
            }

            BigDecimal itemWeight = normalizeWeight(itemDTO.getWeight(), label + "商品" + productCode);
            int qtyPerSet = requirePositive(
                    itemDTO.getQtyPerSet(),
                    label + "商品" + productCode + "的每套数量必须大于0");
            pcsPerSet = safeAdd(pcsPerSet, qtyPerSet, label + "每套总件数过大");
            weightPerSet = weightPerSet.add(
                    itemWeight.multiply(BigDecimal.valueOf(qtyPerSet)));

            SignedOrderSetItem item = new SignedOrderSetItem();
            item.setId(IdWorker.get32UUID());
            item.setOrderDetailId(detail.getId());
            item.setSortNo(itemIndex + 1);
            item.setProductCode(productCode);
            item.setWeight(itemWeight);
            item.setQtyPerSet(qtyPerSet);
            item.setCreateTime(now);
            allSetItems.add(item);
        }

        detail.setProductCode(null);
        detail.setWeight(weightPerSet.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP));
        detail.setTotalSets(totalSets);
        detail.setTotalPcs(safeMultiply(pcsPerSet, totalSets, label + "实物总件数过大"));
        detail.setAmount(calculateAmount(detail.getUnitPrice(), totalSets));
    }

    private Customer findOrCreateCustomer(String rawCustomerId, String rawCustomerName) {
        String customerName = requireText(rawCustomerName, "客户名称不能为空");
        if (StringUtils.hasText(rawCustomerId)) {
            Customer selected = customerMapper.selectById(rawCustomerId.trim());
            if (selected == null) {
                throw new BusinessException(400, "所选客户不存在，请重新选择");
            }
            if (!selected.getCustomerName().equalsIgnoreCase(customerName)) {
                throw new BusinessException(400, "客户ID与客户名称不一致，请重新选择");
            }
            return selected;
        }

        Customer existing = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerName, customerName)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        Customer customer = new Customer();
        customer.setId(IdWorker.get32UUID());
        customer.setCustomerName(customerName);
        customer.setCreateTime(LocalDateTime.now());
        customerMapper.insertIgnore(customer);

        Customer saved = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerName, customerName)
                .last("LIMIT 1"));
        if (saved == null) {
            throw new BusinessException("保存客户失败");
        }
        return saved;
    }

    private SysUser requireActiveSalesman(String rawSalesmanId) {
        String salesmanId = requireText(rawSalesmanId, "业务员不能为空");
        SysUser user = sysUserService.getById(salesmanId);
        if (user == null) {
            throw new BusinessException(400, "所选业务员不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(400, "所选业务员已停用");
        }
        return user;
    }

    private Map<String, BigDecimal> queryCurrencyTotals(SignedOrderPageQueryDTO query) {
        QueryWrapper<SignedOrderMain> wrapper = buildQueryWrapper(query);
        wrapper.eq("status", SignedOrderStatus.ACTIVE.name())
                .select("currency", "COALESCE(SUM(total_amount), 0) AS totalAmount")
                .groupBy("currency");

        Map<String, BigDecimal> totals = new HashMap<>();
        for (Map<String, Object> row : signedOrderMainMapper.selectMaps(wrapper)) {
            Object currency = row.get("currency");
            Object amount = row.get("totalAmount");
            if (amount == null) {
                amount = row.get("totalamount");
            }
            if (currency != null) {
                totals.put(currency.toString(), toBigDecimal(amount));
            }
        }
        return totals;
    }

    private QueryWrapper<SignedOrderMain> buildQueryWrapper(SignedOrderPageQueryDTO query) {
        QueryWrapper<SignedOrderMain> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like("customer_name", query.getCustomerName().trim());
        }
        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like("order_no", query.getOrderNo().trim());
        }
        if (StringUtils.hasText(query.getSalesmanId())) {
            wrapper.eq("salesman_id", query.getSalesmanId().trim());
        }
        if (query.getCreateDateStart() != null) {
            wrapper.ge("create_date", query.getCreateDateStart());
        }
        if (query.getCreateDateEnd() != null) {
            wrapper.le("create_date", query.getCreateDateEnd());
        }
        if (query.getAmountMin() != null) {
            wrapper.ge("total_amount", query.getAmountMin());
        }
        if (query.getAmountMax() != null) {
            wrapper.le("total_amount", query.getAmountMax());
        }
        return wrapper;
    }

    private SignedOrderListVO toListVO(SignedOrderMain main) {
        SignedOrderListVO vo = new SignedOrderListVO();
        BeanUtils.copyProperties(main, vo);
        return vo;
    }

    private SignedOrderLineVO toLineVO(SignedOrderDetail detail,
                                       List<SignedOrderSetItem> setItems) {
        SignedOrderLineVO vo = new SignedOrderLineVO();
        BeanUtils.copyProperties(detail, vo);
        vo.setSetItems(setItems.stream().map(item -> {
            SignedOrderSetItemVO itemVO = new SignedOrderSetItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }

    private void validateQueryRange(SignedOrderPageQueryDTO query) {
        if (query.getCurrent() != null && query.getCurrent() < 1) {
            throw new BusinessException(400, "页码不能小于1");
        }
        if (query.getSize() != null && (query.getSize() < 1 || query.getSize() > 200)) {
            throw new BusinessException(400, "每页数量必须在1到200之间");
        }
        if (query.getCreateDateStart() != null && query.getCreateDateEnd() != null
                && query.getCreateDateStart().isAfter(query.getCreateDateEnd())) {
            throw new BusinessException(400, "创建日期起始值不能晚于结束值");
        }
        if (query.getAmountMin() != null && query.getAmountMax() != null
                && query.getAmountMin().compareTo(query.getAmountMax()) > 0) {
            throw new BusinessException(400, "最小订单金额不能大于最大订单金额");
        }
    }

    private void validateSaveRequest(SignedOrderSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "订单信息不能为空");
        }
        if (dto.getDetailList() == null || dto.getDetailList().isEmpty()) {
            throw new BusinessException(400, "订单商品不能为空");
        }
    }

    private SignedOrderLineType normalizeLineType(String value, int index) {
        String normalized = requireText(value, "第" + (index + 1) + "条商品类型不能为空")
                .toUpperCase(Locale.ROOT);
        try {
            return SignedOrderLineType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "第" + (index + 1) + "条商品类型只能是SINGLE或SET");
        }
    }

    private String normalizeCurrency(String value) {
        String normalized = requireText(value, "币种不能为空").toUpperCase(Locale.ROOT);
        try {
            return SignedOrderCurrency.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "币种只能是CNY或USD");
        }
    }

    private BigDecimal normalizeUnitPrice(BigDecimal value, int index) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "第" + (index + 1) + "条商品单价不能小于0");
        }
        return value.setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeWeight(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, label + "重量必须大于0");
        }
        return value.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAmount(BigDecimal unitPrice, long quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    private int requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    private long safeAdd(long left, long right, String message) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            throw new BusinessException(400, message);
        }
    }

    private long safeMultiply(long left, long right, String message) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException ex) {
            throw new BusinessException(400, message);
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveSalesmanName(SysUser user) {
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        return user.getUsername();
    }

    private int normalizeOptionLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return zeroMoney();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record PreparedLines(
            List<SignedOrderDetail> details,
            List<SignedOrderSetItem> setItems,
            long totalPcs,
            long totalSets,
            BigDecimal totalAmount) {
    }
}
