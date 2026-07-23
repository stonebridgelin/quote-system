package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class UploadResultVO {
    private List<String> successList = new ArrayList<>();
    private List<String> errorList = new ArrayList<>();
}