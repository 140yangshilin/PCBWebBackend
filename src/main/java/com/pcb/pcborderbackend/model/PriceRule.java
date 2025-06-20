package com.pcb.pcborderbackend.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceRule {
    @org.springframework.data.annotation.Id
    private String id;

    private String templateId;

    private String name;
    private boolean enabled;

    // 精确匹配字段条件，如 {"板材类别":"铝基板"}
    private Map<String, String> conditions;

    // 新结构变量表达式
    private List<PreprocessingVar> variables;

    // 最终价格表达式（使用变量名组合）
    private String finalExpression;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreprocessingVar {
        private String name;               // 变量名
        private String expression;         // 表达式
        private List<String> dependsOn;    // 依赖的字段或变量名
        private Map<String, List<String>> triggeredBy; // 触发条件字段 -> [选项值列表]（可选）
    }
    // ✅ 辅助方法：将 conditions 转为标准 JSON 字符串（用于判断是否重复）
    public String getConditionKey() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(conditions != null ? conditions : new HashMap<>());
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
