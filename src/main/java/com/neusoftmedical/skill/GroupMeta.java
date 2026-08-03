package com.neusoftmedical.skill;
import lombok.Data;

import java.util.List;
/**
 * 医疗检查分组元数据实体
 */
@Data
class GroupMeta {
    private Integer id;
    private String groupName;
    private List<FieldMeta> fields;

    public GroupMeta(Integer id, String groupName, List<FieldMeta> fields) {
        this.id = id;
        this.groupName = groupName;
        this.fields = fields;
    }
}