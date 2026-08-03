package com.neusoftmedical.skill;

import lombok.Data;

/**
 * 检查项字段元数据实体
 */
@Data
public class FieldMeta {
    private String fieldTag; // 字段中文名称 tag
    private String fieldCode; // 字段索引 name
    private String searchType;
    private String unit;

    public FieldMeta(String fieldTag, String fieldCode, String searchType, String unit) {
        this.fieldTag = fieldTag;
        this.fieldCode = fieldCode;
        this.searchType = searchType;
        this.unit = unit;
    }
}
