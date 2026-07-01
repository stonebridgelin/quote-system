package com.stonebridge.quotesystem.entity.dto;

import com.stonebridge.quotesystem.entity.OrderDetail;
import com.stonebridge.quotesystem.entity.OrderMain;
import lombok.Data;
import java.util.List;

@Data
public class OrderSaveDTO extends OrderMain {
    private List<OrderDetail> detailList;
}