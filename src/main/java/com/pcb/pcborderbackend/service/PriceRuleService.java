package com.pcb.pcborderbackend.service;

import com.googlecode.aviator.AviatorEvaluator;
import com.pcb.pcborderbackend.model.PriceRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceRuleService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PcbTemplateService templateService;

    private String getCollectionName(String templateId, String lang) {
        String name = templateService.getById(templateId).map(t -> t.getName()).orElse(null);
        if (name == null) throw new RuntimeException("模板不存在: " + templateId);
        return "price." + ("en".equalsIgnoreCase(lang) ? name + "_en" : name);
    }

    public String saveRules(String templateId, String lang, List<PriceRule> rules) {
        String collectionName = getCollectionName(templateId, lang);

        for (PriceRule rule : rules) {
            rule.setTemplateId(templateId);

            Query query = new Query();
            query.addCriteria(Criteria.where("templateId").is(templateId));
            query.addCriteria(Criteria.where("name").is(rule.getName()));
            query.addCriteria(Criteria.where("conditions").is(rule.getConditions()));

            Update update = new Update();
            update.set("enabled", rule.isEnabled());
            update.set("finalExpression", rule.getFinalExpression());
            update.set("variables", rule.getVariables());

            mongoTemplate.upsert(query, update, collectionName);
        }

        return collectionName;
    }

    public List<PriceRule> getRules(String templateId, String lang) {
        String collectionName = getCollectionName(templateId, lang);
        return mongoTemplate.findAll(PriceRule.class, collectionName);
    }

    public Map<String, Object> calculatePrice(String templateId, String lang, Map<String, Object> params) {
        List<PriceRule> rules = getRules(templateId, lang);

        for (PriceRule rule : rules) {
            boolean match = rule.getConditions() == null || rule.getConditions().entrySet().stream()
                    .allMatch(e -> e.getValue().equals(String.valueOf(params.getOrDefault(e.getKey(), ""))));
            if (!match) continue;

            Map<String, Object> context = new HashMap<>(params);
            Map<String, Object> result = new LinkedHashMap<>();

            try {
                for (PriceRule.PreprocessingVar var : rule.getVariables()) {
                    if (var.getTriggeredBy() != null) {
                        boolean triggered = var.getTriggeredBy().entrySet().stream().allMatch(entry -> {
                            String field = entry.getKey();
                            List<String> values = entry.getValue();
                            Object userValue = params.get(field);
                            return userValue != null && values.contains(userValue.toString());
                        });
                        if (!triggered) continue;
                    }
                    Object val = AviatorEvaluator.execute(var.getExpression(), context);
                    context.put(var.getName(), val);
                    result.put(var.getName(), val);
                }

                Object total = AviatorEvaluator.execute(rule.getFinalExpression(), context);
                result.put("totalPrice", total);
                result.put("ruleName", rule.getName());
                return result;

            } catch (Exception e) {
                throw new RuntimeException("表达式执行失败: " + e.getMessage());
            }
        }

        throw new RuntimeException("没有匹配的价格规则");
    }

    public Optional<PriceRule> findOneRule(String templateId, String lang, String name, Map<String, String> conditions) {
        String collectionName = getCollectionName(templateId, lang);

        Query query = new Query();
        query.addCriteria(Criteria.where("templateId").is(templateId));
        query.addCriteria(Criteria.where("name").is(name));
        query.addCriteria(Criteria.where("conditions").is(conditions));

        PriceRule rule = mongoTemplate.findOne(query, PriceRule.class, collectionName);
        return Optional.ofNullable(rule);
    }

    public boolean deleteRuleById(String templateId, String lang, String ruleId) {
        String collectionName = getCollectionName(templateId, lang);

        Query query = new Query(Criteria.where("_id").is(ruleId));
        return mongoTemplate.remove(query, PriceRule.class, collectionName).getDeletedCount() > 0;
    }


    public Optional<PriceRule> findRuleById(String templateId, String lang, String ruleId) {
        String collectionName = getCollectionName(templateId, lang);

        Query query = new Query(Criteria.where("_id").is(ruleId));
        PriceRule rule = mongoTemplate.findOne(query, PriceRule.class, collectionName);
        return Optional.ofNullable(rule);
    }

    public List<PriceRule> findRulesByConditions(String templateId, String lang, Map<String, String> conditions) {
        String collectionName = getCollectionName(templateId, lang);

        List<PriceRule> allRules = mongoTemplate.findAll(PriceRule.class, collectionName);

        return allRules.stream().filter(rule -> {
            if (rule.getConditions() == null) return false;
            return conditions.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(rule.getConditions().get(entry.getKey())));
        }).collect(Collectors.toList());
    }

}
