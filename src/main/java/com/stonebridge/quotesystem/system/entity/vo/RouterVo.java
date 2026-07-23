package com.stonebridge.quotesystem.system.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RouterVo {
    private String path;
    private String name;
    private Boolean hidden;
    private String component;
    private Boolean alwaysShow;
    private MetaVo meta;
    private List<RouterVo> children = new ArrayList<>();
}
