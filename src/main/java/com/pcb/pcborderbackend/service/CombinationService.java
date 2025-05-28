package com.pcb.pcborderbackend.service;

import com.mongodb.client.result.DeleteResult;
import com.pcb.pcborderbackend.model.PcbTemplate;
import com.pcb.pcborderbackend.model.PcbTemplateI18n;
import com.pcb.pcborderbackend.repository.PcbTemplateI18nRepository;
import com.pcb.pcborderbackend.repository.PcbTemplateRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CombinationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PcbTemplateRepository templateRepository;
    @Autowired
    private PcbTemplateI18nRepository i18nRepository;


    public void generateAndSaveCombinationsById(Map<String, Object> request) {
        String templateId = (String) request.get("template_id");
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("缺少 template_id");
        }

        Optional<PcbTemplateI18n> optionalTemplate = i18nRepository.findById(templateId);
        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("未找到模板 id 对应的中英文模板数据");
        }

        PcbTemplateI18n template = optionalTemplate.get();
        String collectionNameZh = template.getNameZh();            // 中文集合名
        String collectionNameEn = template.getNameZh() + "_en";    // 英文集合名

        // 构建字段名与字段值映射
        Map<String, String> zhToEnKeyMap = new HashMap<>();
        Map<String, Map<String, String>> zhValueToEnMap = new HashMap<>();

        for (PcbTemplateI18n.Category category : template.getCategories()) {
            for (PcbTemplateI18n.Field field : category.getFields()) {
                String keyZh = field.getKeyZh();
                String keyEn = field.getKeyEn();
                zhToEnKeyMap.put(keyZh, keyEn);

                Map<String, String> valueMap = new HashMap<>();
                for (PcbTemplateI18n.Option opt : field.getOptions()) {
                    valueMap.put(opt.getNameZh(), opt.getNameEn());
                }
                zhValueToEnMap.put(keyZh, valueMap);
            }
        }

        // 解析前端字段 & 构造中英字段值组合
        Map<String, List<String>> zhFields = new HashMap<>();
        Map<String, List<String>> enFields = new HashMap<>();

        for (Map.Entry<String, Object> entry : request.entrySet()) {
            if ("template_id".equals(entry.getKey())) continue;

            String zhKey = entry.getKey();
            String enKey = zhToEnKeyMap.get(zhKey);
            if (enKey == null) {
                throw new IllegalArgumentException("字段未找到英文映射: " + zhKey);
            }

            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                List<String> zhValues = list.stream().map(Object::toString).toList();
                zhFields.put(zhKey, zhValues);

                Map<String, String> valueMap = zhValueToEnMap.get(zhKey);
                if (valueMap == null) {
                    throw new IllegalArgumentException("字段值映射不存在: " + zhKey);
                }

                List<String> enValues = zhValues.stream()
                        .map(v -> {
                            String en = valueMap.get(v);
                            if (en == null) {
                                throw new IllegalArgumentException("字段值未找到英文映射: " + zhKey + " → " + v);
                            }
                            return en;
                        }).toList();

                enFields.put(enKey, enValues);
            }
        }

        // 生成组合
        List<Map<String, String>> zhCombinations = cartesianProduct(zhFields);
        List<Map<String, String>> enCombinations = cartesianProduct(enFields);

        // 写入组合，避免重复
        List<ObjectId> ids = new ArrayList<>();

        // 中文写入
        for (Map<String, String> combination : zhCombinations) {
            Query existQuery = new Query();
            combination.forEach((k, v) -> existQuery.addCriteria(Criteria.where(k).is(v)));

            boolean exists = mongoTemplate.exists(existQuery, collectionNameZh);
            if (exists) continue;

            ObjectId id = new ObjectId();
            combination.put("_id", id.toHexString());
            combination.put("template_id", templateId);
            combination.put("template_name", collectionNameZh);
            mongoTemplate.insert(new Document(combination), collectionNameZh);
            ids.add(id);
        }

        // 英文写入
        int i = 0;
        for (Map<String, String> combination : enCombinations) {
            Query existQuery = new Query();
            combination.forEach((k, v) -> existQuery.addCriteria(Criteria.where(k).is(v)));

            boolean exists = mongoTemplate.exists(existQuery, collectionNameEn);
            if (exists) continue;

            ObjectId id = (i < ids.size()) ? ids.get(i++) : new ObjectId(); // 保底兜住
            combination.put("_id", id.toHexString());
            combination.put("template_id", templateId);
            combination.put("template_name", collectionNameEn);
            mongoTemplate.insert(new Document(combination), collectionNameEn);
        }
    }

    public Map<String, Object> getCombinationById(String templateId, String id) {
        if (templateId == null || templateId.isBlank() || id == null || id.isBlank()) {
            throw new IllegalArgumentException("template_id 和 id 都不能为空");
        }

        Optional<PcbTemplate> template = templateRepository.findById(templateId);
        if (template.isEmpty()) {
            throw new IllegalArgumentException("未找到对应模板");
        }

        String collectionName = template.get().getName();

        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)));
        Document doc = mongoTemplate.findOne(query, Document.class, collectionName);
        if (doc == null) {
            throw new IllegalArgumentException("未找到指定组合");
        }

        // 转 Map 返回
        Map<String, Object> result = new HashMap<>(doc);
        result.remove("_id");
        result.put("id", id); // 显式返回 id 字段

        return result;
    }


    public Map<String, Object> queryValidOptionsById(Map<String, Object> request, String lang) {
        String templateId = (String) request.get("template_id");
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("缺少 template_id");
        }

        // 获取模板名称
        Optional<PcbTemplate> optionalTemplate = templateRepository.findById(templateId);
        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("模板不存在");
        }

        String baseName = optionalTemplate.get().getName();
        String collectionName = "en".equalsIgnoreCase(lang) ? baseName + "_en" : baseName;

        // 构建查询条件
        Query query = new Query();
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            if ("template_id".equals(entry.getKey())) continue;
            query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue()));
        }

        List<Document> matched = mongoTemplate.find(query, Document.class, collectionName);

        // 聚合可选项
        Map<String, Set<Object>> resultMap = new HashMap<>();
        for (Document doc : matched) {
            for (String key : doc.keySet()) {
                if (List.of("_id", "template_id", "template_name").contains(key)) continue;
                if (request.containsKey(key)) continue; // 已作为条件的字段跳过
                resultMap.computeIfAbsent(key, k -> new HashSet<>()).add(doc.get(key));
            }
        }

        Map<String, Object> result = new HashMap<>();
        resultMap.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }


    public void deleteCombinationByFields(Map<String, Object> request) {
        String templateId = (String) request.get("template_id");
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("template_id 不可为空");
        }

        Optional<PcbTemplate> optional = templateRepository.findById(templateId);
        Optional<PcbTemplateI18n> i18nOpt = i18nRepository.findById(templateId);

        if (optional.isEmpty() || i18nOpt.isEmpty()) {
            throw new IllegalArgumentException("模板不存在");
        }

        String collectionZh = optional.get().getName();
        String collectionEn = collectionZh + "_en";

        PcbTemplateI18n i18nTemplate = i18nOpt.get();

        // 字段映射：keyZh → keyEn
        Map<String, String> zhToEnKeyMap = new HashMap<>();
        Map<String, Map<String, String>> zhValToEnMap = new HashMap<>();

        for (PcbTemplateI18n.Category cat : i18nTemplate.getCategories()) {
            for (PcbTemplateI18n.Field field : cat.getFields()) {
                zhToEnKeyMap.put(field.getKeyZh(), field.getKeyEn());
                Map<String, String> valMap = new HashMap<>();
                for (PcbTemplateI18n.Option opt : field.getOptions()) {
                    valMap.put(opt.getNameZh(), opt.getNameEn());
                }
                zhValToEnMap.put(field.getKeyZh(), valMap);
            }
        }

        // 构造中文查询条件
        Query zhQuery = new Query();
        // 构造英文查询条件
        Query enQuery = new Query();

        for (Map.Entry<String, Object> entry : request.entrySet()) {
            if ("template_id".equals(entry.getKey())) continue;

            // 中文字段和值
            String zhKey = entry.getKey();
            String zhVal = entry.getValue().toString();

            zhQuery.addCriteria(Criteria.where(zhKey).is(zhVal));

            // 映射英文字段和值
            String enKey = zhToEnKeyMap.get(zhKey);
            String enVal = zhValToEnMap.get(zhKey).get(zhVal);

            if (enKey == null || enVal == null) {
                throw new IllegalArgumentException("字段或值映射失败: " + zhKey + " → " + zhVal);
            }

            enQuery.addCriteria(Criteria.where(enKey).is(enVal));
        }

        // 执行删除
        mongoTemplate.remove(zhQuery, collectionZh);
        mongoTemplate.remove(enQuery, collectionEn);
    }



    private List<Map<String, String>> cartesianProduct(Map<String, List<String>> input) {
        List<Map<String, String>> result = new ArrayList<>();
        cartesianHelper(input, new LinkedHashMap<>(), result);
        return result;
    }

    private void cartesianHelper(Map<String, List<String>> input,
                                 Map<String, String> current,
                                 List<Map<String, String>> result) {
        if (current.size() == input.size()) {
            result.add(new LinkedHashMap<>(current));
            return;
        }

        String nextKey = new ArrayList<>(input.keySet()).get(current.size());
        for (String value : input.get(nextKey)) {
            current.put(nextKey, value);
            cartesianHelper(input, current, result);
            current.remove(nextKey);
        }
    }
}
