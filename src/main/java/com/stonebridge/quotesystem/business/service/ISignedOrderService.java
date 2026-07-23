package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.dto.SignedOrderPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSaveDTO;
import com.stonebridge.quotesystem.business.entity.vo.CustomerOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SalesmanOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderPageVO;

import java.util.List;

public interface ISignedOrderService {

    SignedOrderPageVO page(SignedOrderPageQueryDTO queryDTO);

    String create(SignedOrderSaveDTO dto);

    SignedOrderDetailVO detail(String id);

    void cancel(String id, String cancelReason);

    List<CustomerOptionVO> customerOptions(String keyword, Integer limit);

    List<SalesmanOptionVO> salesmanOptions(String keyword, Integer limit);
}
