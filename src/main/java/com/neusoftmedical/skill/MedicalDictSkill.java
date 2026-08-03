package com.neusoftmedical.skill;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * 医疗检查字典Skill
 * 提供两类查询能力：
 * 1. 根据字段名称查字段编码(name)
 * 2. 根据检查分组名称+字段名称精准查字段编码(name)
 */
@Component
public class MedicalDictSkill {

    // 静态初始化完整元数据（对应附件json结构）
    private static final List<GroupMeta> MEDICAL_META_LIST;

    static {
        MEDICAL_META_LIST = List.of(
                new GroupMeta(19, "病程记录", List.of(
                        new FieldMeta("专科检查", "bc_zkjc", "fulltext", null),
                        new FieldMeta("既往病史", "bc_jwbs", "fulltext", null),
                        new FieldMeta("现病史", "bc_xbs", "fulltext", null),
                        new FieldMeta("入院诊断", "bc_rzdj", "fulltext", null),
                        new FieldMeta("诊疗经过", "bc_zljg", "fulltext", null)
                )),
                new GroupMeta(1, "生化全套", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("球蛋白", "20112", "float", "g/L"),
                        new FieldMeta("尿酸", "20117", "float", "μmol/L"),
                        new FieldMeta("钾", "20126", "float", "mmol/L"),
                        new FieldMeta("总胆汁酸*", "20111", "float", "μmol/L"),
                        new FieldMeta("直接胆红素", "20102", "float", "μmol/L"),
                        new FieldMeta("钠", "20127", "float", "mmol/L"),
                        new FieldMeta("肌酐", "20115", "float", "μmol/L"),
                        new FieldMeta("谷氨酰转肽酶", "20106", "float", "U/L"),
                        new FieldMeta("白蛋白", "20110", "float", "g/L"),
                        new FieldMeta("尿素", "20114", "float", "mmol/L"),
                        new FieldMeta("天冬氨酸氨基转移酶", "20105", "float", "U/L"),
                        new FieldMeta("间接胆红素", "20103", "float", "μmol/L"),
                        new FieldMeta("丙氨酸氨基转移酶", "20104", "float", "U/L"),
                        new FieldMeta("碱性磷酸酶", "20108", "float", "U/L"),
                        new FieldMeta("腺苷脱氨酶*", "20188", "float", "U/L"),
                        new FieldMeta("钙", "20129", "float", "mmol/L"),
                        new FieldMeta("总蛋白", "20109", "float", "g/L"),
                        new FieldMeta("氯", "20128", "float", "mmol/L"),
                        new FieldMeta("白蛋白/球蛋白", "20113", "float", null),
                        new FieldMeta("磷", "20130", "float", "mmol/L"),
                        new FieldMeta("前白蛋白*", "20209", "float", "mg/L"),
                        new FieldMeta("总胆红素", "20101", "float", "μmol/L"),
                        new FieldMeta("镁*", "20131", "float", "mmol/L")
                )),
                new GroupMeta(2, "肝纤四项", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("IV型胶原*", "Col IV", "float", "ng/ml"),
                        new FieldMeta("透明质酸*", "HA", "float", "ng/ml"),
                        new FieldMeta("III型前胶原N端肽*", "PIIINP", "float", "ng/ml"),
                        new FieldMeta("层黏连蛋白*", "LN", "float", "ng/ml")
                )),
                new GroupMeta(3, "血凝五项（急）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("纤维蛋白原*", "60144", "float", "g/L"),
                        new FieldMeta("抗凝血酶原III活性*", "60146", "float", "%"),
                        new FieldMeta("活化部分凝血活酶时间*", "60142", "float", "sec"),
                        new FieldMeta("凝血酶原时间*", "60141", "float", "sec"),
                        new FieldMeta("凝血酶原时间(INR)*", "60145", "float", null),
                        new FieldMeta("凝血酶时间*", "60143", "float", "sec")
                )),
                new GroupMeta(4, "输血常规(检验科)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("乙肝表面抗体", "0400013", "float", "mIU/ml"),
                        new FieldMeta("人免疫缺陷病毒抗体/p24", "0400017", "float", "S/CO"),
                        new FieldMeta("乙肝e抗原", "0400014", "float", "S/CO"),
                        new FieldMeta("丙型肝炎病毒抗体IgG", "0400019", "float", "S/CO"),
                        new FieldMeta("乙肝表面抗原", "0400012", "float", "IU/ml"),
                        new FieldMeta("乙肝核心抗体", "0400016", "float", "S/CO"),
                        new FieldMeta("乙肝e抗体", "0400015", "float", "S/CO"),
                        new FieldMeta("梅毒螺旋体抗体", "0400018", "float", "S/CO")
                )),
                new GroupMeta(5, "血细胞分析（急）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("红细胞分布宽度", "0101038", "float", "%"),
                        new FieldMeta("血小板计数", "10128", "float", "10^9/L"),
                        new FieldMeta("淋巴细胞百分比", "10108", "float", "%"),
                        new FieldMeta("单核细胞百分比", "10109", "float", "%"),
                        new FieldMeta("血小板比积", "0101039", "float", "%"),
                        new FieldMeta("嗜碱性细胞计数", "10106", "float", "10^9/L"),
                        new FieldMeta("嗜酸性细胞计数", "10105", "float", "10^9/L"),
                        new FieldMeta("平均红细胞血红蛋白量", "10125", "float", "pg"),
                        new FieldMeta("白细胞计数", "10101", "float", "10^9/L"),
                        new FieldMeta("平均红细胞血红蛋白浓度", "10126", "float", "g/L"),
                        new FieldMeta("淋巴细胞计数", "10102", "float", "10^9/L"),
                        new FieldMeta("单核细胞计数", "10103", "float", "10^9/L"),
                        new FieldMeta("中性粒细胞计数", "10104", "float", "10^9/L"),
                        new FieldMeta("血红蛋白量", "10122", "float", "g/L"),
                        new FieldMeta("平均血小板体积", "0101040", "float", "fl"),
                        new FieldMeta("嗜酸性细胞百分比", "10111", "float", "%"),
                        new FieldMeta("血小板分布宽度", "0101041", "float", null),
                        new FieldMeta("红细胞计数", "10121", "float", "10^12/L"),
                        new FieldMeta("嗜碱性细胞百分比", "10112", "float", "%"),
                        new FieldMeta("中性粒细胞百分比", "10110", "float", "%"),
                        new FieldMeta("平均红细胞体积", "10124", "float", "fl"),
                        new FieldMeta("红细胞比积", "10123", "float", "L/L")
                )),
                new GroupMeta(6, "HBVDNA核酸检测（普敏）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("乙型肝炎病毒核酸", "0701043", "float", "IU/ml")
                )),
                new GroupMeta(7, "胸痛四项（急）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("肌酸激酶同工酶MB质量", "CK-MB", "float", "ng/mL"),
                        new FieldMeta("高敏肌钙蛋白T", "hs-cTnT", "float", "pg/mL"),
                        new FieldMeta("肌红蛋白", "Myo", "float", "ng/mL"),
                        new FieldMeta("B型氨基端尿钠肽原", "NT-ProBNP", "float", "pg/mL")
                )),
                new GroupMeta(8, "肿瘤全套(男性)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("神经原烯醇化酶*", "80177", "float", "ng/ml"),
                        new FieldMeta("CA72-4*", "80175", "float", "U/ml"),
                        new FieldMeta("铁蛋白", "80117", "float", "ng/mL"),
                        new FieldMeta("CYFRA211*", "80176", "float", "ng/ml"),
                        new FieldMeta("总前列腺特异抗原*", "80120", "float", "ng/mL"),
                        new FieldMeta("SCCA*", "80189", "float", "ng/ml"),
                        new FieldMeta("糖类抗原CA199*", "80116", "float", "IU/mL"),
                        new FieldMeta("癌胚抗原", "80115", "float", "ng/mL"),
                        new FieldMeta("肿瘤特异生长因子", "80143", "float", "U/mL"),
                        new FieldMeta("甲胎蛋白", "80114", "float", "μg/L"),
                        new FieldMeta("糖类抗原CA153*", "80119", "float", "U/mL"),
                        new FieldMeta("异常凝血酶原测定", "80203", "float", "mAU/mL"),
                        new FieldMeta("糖类抗原CA125*", "80118", "float", "U/mL"),
                        new FieldMeta("游离前列腺特异抗原*", "80156", "float", "ng/ml")
                )),
                new GroupMeta(9, "粪便常规+隐血(检验科)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("隐血*", "10276", "float", null),
                        new FieldMeta("血吸虫卵", "10631", "float", null),
                        new FieldMeta("姜片虫卵", "10634", "float", null),
                        new FieldMeta("性状", "10272", "float", null),
                        new FieldMeta("肺吸虫卵", "10632", "float", null),
                        new FieldMeta("吞噬细胞", "10620", "float", null),
                        new FieldMeta("白细胞", "10618", "float", "个/HP"),
                        new FieldMeta("蛔虫卵", "10627", "float", null),
                        new FieldMeta("颜色", "10271", "float", null),
                        new FieldMeta("真菌", "10622", "float", null),
                        new FieldMeta("淀粉颗粒", "10648", "float", null),
                        new FieldMeta("夏科雷登结晶", "10637", "float", null),
                        new FieldMeta("其他", "10653", "float", null),
                        new FieldMeta("带绦虫卵", "10635", "float", null),
                        new FieldMeta("蛲虫卵", "10630", "float", null),
                        new FieldMeta("脂肪球", "10621", "float", null),
                        new FieldMeta("红细胞", "10619", "float", "个/HP"),
                        new FieldMeta("钩虫卵", "10628", "float", null),
                        new FieldMeta("原虫", "10623", "float", null),
                        new FieldMeta("鞭虫卵", "10629", "float", null),
                        new FieldMeta("其他虫卵", "10636", "float", null),
                        new FieldMeta("肝吸虫卵", "10633", "float", null)
                )),
                new GroupMeta(10, "尿常规检测＋沉渣定量(临床检测中心)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("尿胆原", "0102018", "float", null),
                        new FieldMeta("白细胞", "0102034", "float", "个/ul"),
                        new FieldMeta("鳞状上皮细胞", "0102036", "float", "个/ul"),
                        new FieldMeta("比重", "0102024", "float", null),
                        new FieldMeta("亚硝酸盐", "0102021", "float", null),
                        new FieldMeta("白细胞团", "0102046", "float", "个/ul"),
                        new FieldMeta("非晶形结晶", "0102045", "float", null),
                        new FieldMeta("粘液丝", "0102038", "float", "个/ul"),
                        new FieldMeta("细菌", "0102035", "float", "个/ul"),
                        new FieldMeta("酮体", "0102019", "float", null),
                        new FieldMeta("pH", "0102023", "float", null),
                        new FieldMeta("维生素C", "0102028", "float", null),
                        new FieldMeta("白细胞酯酶", "0102025", "float", null),
                        new FieldMeta("吞噬细胞", "0102048", "float", "个/ul"),
                        new FieldMeta(".", "0102050", "float", null),
                        new FieldMeta("真菌", "0102047", "float", "个/ul"),
                        new FieldMeta("颗粒管型", "0102040", "float", "个/ul"),
                        new FieldMeta("胆红素", "0102017", "float", null),
                        new FieldMeta("尿液浊度", "0102027", "float", null),
                        new FieldMeta("非鳞状上皮细胞", "0102037", "float", "个/ul"),
                        new FieldMeta("隐血", "0102016", "float", null),
                        new FieldMeta("草酸钙结晶", "0102042", "float", "个/ul"),
                        new FieldMeta("未分类管型", "0102041", "float", "个/ul"),
                        new FieldMeta("尿液颜色", "0102026", "float", null),
                        new FieldMeta(".", "0102049", "float", null),
                        new FieldMeta("蛋白质", "0102020", "float", null),
                        new FieldMeta("葡萄糖", "0102022", "float", null),
                        new FieldMeta("磷酸铵镁结晶", "0102044", "float", "个/uL"),
                        new FieldMeta("透明管型", "0102039", "float", "个/ul"),
                        new FieldMeta("红细胞", "0102033", "float", "个/ul"),
                        new FieldMeta("尿酸结晶", "0102043", "float", "个/ul")
                )),
                new GroupMeta(11, "急诊血浆氨测定(干化学法)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("血氨测定干片", "60111", "float", "μmol/L")
                )),
                new GroupMeta(12, "淋巴细胞免疫检查点检测", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("CD3+CD28+", "0204078", "float", "个/uL"),
                        new FieldMeta("CD8+CD28+", "0204081", "float", "个/uL"),
                        new FieldMeta("CD8+CD28+", "0204076", "float", "%"),
                        new FieldMeta("CD3+PD1+", "0204072", "float", "%"),
                        new FieldMeta("CD8+PD1+", "0204075", "float", "%"),
                        new FieldMeta("CD3+CD8+", "0204083", "float", "个/uL"),
                        new FieldMeta("CD3+CD4+", "304403", "float", "%"),
                        new FieldMeta("CD3+", "304411", "float", "个/uL"),
                        new FieldMeta("CD4+CD28+", "0204074", "float", "%"),
                        new FieldMeta("CD3+CD28", "0200029", "float", "%"),
                        new FieldMeta("CD4+CD28+", "0204080", "float", "个/uL"),
                        new FieldMeta("CD8+PD1+", "CD8+PD1+", "float", "个/uL"),
                        new FieldMeta("CD3+", "304342", "float", "%"),
                        new FieldMeta("CD3+CD8+", "304404", "float", "%"),
                        new FieldMeta("CD3+CD4+", "0204082", "float", "个/uL"),
                        new FieldMeta("CD4+PD1+", "0204079", "float", "个/uL"),
                        new FieldMeta("CD3+PD1+", "0204077", "float", "个/uL"),
                        new FieldMeta("CD4+PD1+", "0204073", "float", "%")
                )),
                new GroupMeta(13, "肿瘤全套(女性)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("CYFRA211*", "80176", "float", "ng/ml"),
                        new FieldMeta("糖类抗原CA153*", "80119", "float", "U/mL"),
                        new FieldMeta("肿瘤特异生长因子", "80143", "float", "U/mL"),
                        new FieldMeta("糖类抗原CA199*", "80116", "float", "IU/mL"),
                        new FieldMeta("铁蛋白", "80117", "float", "ng/mL"),
                        new FieldMeta("癌胚抗原", "80115", "float", "ng/mL"),
                        new FieldMeta("甲胎蛋白", "80114", "float", "μg/L"),
                        new FieldMeta("SCCA*", "80189", "float", "ng/ml"),
                        new FieldMeta("绒毛膜促性腺激素*", "80122", "float", "mIU/mL"),
                        new FieldMeta("神经原烯醇化酶*", "80177", "float", "ng/ml"),
                        new FieldMeta("糖类抗原CA125*", "80118", "float", "U/mL"),
                        new FieldMeta("CA72-4*", "80175", "float", "U/ml")
                )),
                new GroupMeta(14, "血凝七项组套（急）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("凝血酶时间*", "60143", "float", "sec"),
                        new FieldMeta("纤维蛋白降解物*", "60148", "float", "mg/L"),
                        new FieldMeta("纤维蛋白原*", "60144", "float", "g/L"),
                        new FieldMeta("抗凝血酶原III活性*", "60146", "float", "%"),
                        new FieldMeta("凝血酶原时间*", "60141", "float", "sec"),
                        new FieldMeta("D-二聚体*", "0305002", "float", "ug/mL"),
                        new FieldMeta("凝血酶原时间(INR)*", "60145", "float", null),
                        new FieldMeta("活化部分凝血活酶时间*", "60142", "float", "sec")
                )),
                new GroupMeta(15, "甲功全套(核医学科)", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("促甲状腺激素", "80105", "float", "mIU/L"),
                        new FieldMeta("游离T3", "80103", "float", "pmol/L"),
                        new FieldMeta("甲状腺素", "80102", "float", "ug/dL"),
                        new FieldMeta("游离T4", "80104", "float", "pmol/L"),
                        new FieldMeta("三碘甲状原氨酸", "80101", "float", "ng/mL")
                )),
                new GroupMeta(16, "生化全套（急）", List.of(
                        new FieldMeta("检验时间", "occurred", "date", null),
                        new FieldMeta("阴离子间隙", "60108", "float", "mmol/L"),
                        new FieldMeta("二氧化碳测定干片", "60107", "float", "mmol/L"),
                        new FieldMeta("白蛋白测定干片*", "60127", "float", "g/L"),
                        new FieldMeta("钠离子测定干片*", "60103", "float", "mmol/L"),
                        new FieldMeta("总蛋白测定干片*", "60126", "float", "g/L"),
                        new FieldMeta("钾离子测定干片*", "60102", "float", "mmol/L"),
                        new FieldMeta("氯离子测定干片*", "60104", "float", "mmol/L"),
                        new FieldMeta("钙测定干片*", "60114", "float", "mmol/L"),
                        new FieldMeta("丙氨酸氨基转移酶测定干", "60110", "float", "U/L"),
                        new FieldMeta("总胆红素测定干片*", "60123", "float", "μmol/L"),
                        new FieldMeta("天冬氨酸氨基转移酶测定", "60109", "float", "U/L"),
                        new FieldMeta("尿素氮测定干片*", "60121", "float", "mmol/L"),
                        new FieldMeta("肌酐测定干片*", "60122", "float", "μmol/L"),
                        new FieldMeta("磷测定干片*", "60118", "float", "mmol/L")
                )),
                new GroupMeta(17, "CT检查", List.of(
                        new FieldMeta("检查项目", "examItem", "text", null),
                        new FieldMeta("检查所见", "examFindings", "fulltext", null),
                        new FieldMeta("检查结论", "examResult", "fulltext", null),
                        new FieldMeta("检查时间", "occurred", "date", null)
                )),
                new GroupMeta(18, "MRI检查", List.of(
                        new FieldMeta("检查项目", "examItem", "text", null),
                        new FieldMeta("检查所见", "examFindings", "fulltext", null),
                        new FieldMeta("检查结论", "examResult", "fulltext", null),
                        new FieldMeta("检查时间", "occurred", "date", null)
                ))
        );
    }

    /**
     * Skill1：根据检查类型中文名称(groupName)查询对应的索引(id)
     * @param groupName 检查类型名称，例如：生化全套、CT检查、病程记录、血细胞分析（急）
     * @return 匹配到的检查类型索引编码；无匹配返回"未查询到该检查类型名称"
     */
    @Tool(description = "根据检查类型名称查询对应索引编码，仅传入检查类型名即可")
    public String getGroupId(
            @ToolParam(description = "检查类型名称，例如：生化全套、CT检查、病程记录、血细胞分析（急）") String groupName
    ) {
        List<Integer> groupIds = MEDICAL_META_LIST.stream()
                .filter(group -> group.getGroupName().equals(groupName) || group.getGroupName().contains(groupName))
                .map(GroupMeta::getId)
                .toList();

        if (groupIds.isEmpty()) {
            return "未查询到检查类型名称【" + groupName + "】对应的字段索引编码";
        }
        return "检查类型【" + groupName + "】索引编码：" + groupIds.getFirst();
    }

    /**
     * Skill2：仅根据字段中文名称(tag)查询对应的字段索引(name)
     * @param fieldName 检验/病历字段中文名称，如：肌酐、白细胞、检查所见
     * @return 匹配到的字段编码；无匹配返回"未查询到该字段名称"
     */
    @Tool(description = "根据医疗字段中文名称查询对应存储字段索引编码，仅传入字段名即可，不区分检查类型")
    public String getFieldCodeByFieldName(
            @ToolParam(description = "医疗检验、病历、影像的字段中文名称，例如：肌酐、白细胞、检查所见、现病史") String fieldName
    ) {
        List<String> matchCodes = MEDICAL_META_LIST.stream()
                .flatMap(group -> group.getFields().stream())
                .filter(field -> field.getFieldTag().equals(fieldName) || field.getFieldTag().contains(fieldName))
                .map(FieldMeta::getFieldCode)
                .toList();

        if (matchCodes.isEmpty()) {
            return "未查询到字段名称【" + fieldName + "】对应的字段索引编码";
        }
        return "字段【" + fieldName + "】索引编码：" + matchCodes.getFirst();
    }

    /**
     * Skill3：根据检查类型名称+字段中文名称精准查询字段索引name
     * @param groupName 检查分组名称，如：生化全套、CT检查、病程记录
     * @param fieldName 分组下的字段中文名称，如：肌酐、检查结论
     * @return 精准匹配的字段索引；无匹配返回提示
     */
    @Tool(description = "精准查询：传入检查类型名称和该类型下的字段中文名称，返回唯一对应的字段索引编码name，解决同名字段多分组冲突问题")
    public String getFieldCodeByGroupAndFieldName(
            @ToolParam(description = "检查类型名称，例如：生化全套、CT检查、病程记录、血细胞分析（急）") String groupName,
            @ToolParam(description = "该分组下的字段中文名称，例如：肌酐、检查结论、既往病史") String fieldName
    ) {
        Optional<GroupMeta> targetGroupOpt = MEDICAL_META_LIST.stream()
                .filter(g -> g.getGroupName().equals(groupName) || g.getGroupName().contains(groupName))
                .findFirst();

        if (targetGroupOpt.isEmpty()) {
            return "不存在名称为【" + groupName + "】的检查分组";
        }
        GroupMeta targetGroup = targetGroupOpt.get();

        Optional<FieldMeta> targetFieldOpt = targetGroup.getFields().stream()
                .filter(f -> f.getFieldTag().equals(fieldName) || f.getFieldTag().contains(fieldName))
                .findFirst();

        if (targetFieldOpt.isEmpty()) {
            return "检查类型【" + groupName + "】下不存在字段【" + fieldName + "】";
        }
        FieldMeta field = targetFieldOpt.get();
        return String.format("检查类型【%s】中字段【%s】对应的索引编码：%s",
                groupName, fieldName, field.getFieldCode());
    }
}
