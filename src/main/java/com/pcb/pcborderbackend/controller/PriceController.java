package com.pcb.pcborderbackend.controller;

import com.pcb.pcborderbackend.model.PriceRule;
import com.pcb.pcborderbackend.service.PriceRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/price")
public class PriceController {

    @Autowired
    private PriceRuleService priceRuleService;

    // 保存规则（自动覆盖）
    @PostMapping("/rules/{id}")
    public ResponseEntity<?> saveRules(@PathVariable String id,
                                       @RequestParam(defaultValue = "ch") String lang,
                                       @RequestBody List<PriceRule> rules) {
        try {
            priceRuleService.saveRules(id, lang, rules);
            return ResponseEntity.ok("规则已保存（自动覆盖相同条件）: " + rules.size() + " 条");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("保存失败: " + e.getMessage());
        }
    }

    // 获取规则
    @GetMapping("/rules/{id}")
    public ResponseEntity<?> getRules(@PathVariable String id,
                                      @RequestParam(defaultValue = "ch") String lang) {
        try {
            List<PriceRule> rules = priceRuleService.getRules(id, lang);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // 计算价格
    @PostMapping("/calculate/{id}")
    public ResponseEntity<?> calculate(@PathVariable String id,
                                       @RequestParam(defaultValue = "ch") String lang,
                                       @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = priceRuleService.calculatePrice(id, lang, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // 查询具体某条规则
    @PostMapping("/rule/query/{id}")
    public ResponseEntity<?> getOneRule(@PathVariable String id,
                                        @RequestParam(defaultValue = "ch") String lang,
                                        @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");

            Map<String, Object> rawConditions = (Map<String, Object>) body.get("conditions");
            Map<String, String> conditions = rawConditions.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().toString()
                    ));

            Optional<PriceRule> ruleOpt = priceRuleService.findOneRule(id, lang, name, conditions);

            if (ruleOpt.isPresent()) {
                return ResponseEntity.ok(ruleOpt.get());
            } else {
                return ResponseEntity.status(404).body("未找到该规则");
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body("查询失败: " + e.getMessage());
        }
    }



    // 删除某条规则
    @DeleteMapping("/rule/{templateId}/{ruleId}")
    public ResponseEntity<?> deleteRuleById(@PathVariable String templateId,
                                            @PathVariable String ruleId,
                                            @RequestParam(defaultValue = "ch") String lang) {
        try {
            boolean deleted = priceRuleService.deleteRuleById(templateId, lang, ruleId);
            if (deleted) {
                return ResponseEntity.ok("删除成功");
            } else {
                return ResponseEntity.status(404).body("未找到规则或删除失败");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("删除失败: " + e.getMessage());
        }
    }


    // 根据模版ID和规则ID获取规则信息
    @GetMapping("/rule/{templateId}/{ruleId}")
    public ResponseEntity<?> getRuleById(@PathVariable String templateId,
                                         @PathVariable String ruleId,
                                         @RequestParam(defaultValue = "ch") String lang) {
        try {
            Optional<PriceRule> ruleOpt = priceRuleService.findRuleById(templateId, lang, ruleId);
            if (ruleOpt.isPresent()) {
                return ResponseEntity.ok(ruleOpt.get());
            } else {
                return ResponseEntity.status(404).body("未找到规则");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("查询失败: " + e.getMessage());
        }
    }

    //根据模版ID和条件获得规则信息
    @PostMapping("/rules/search/{templateId}")
    public ResponseEntity<?> searchRulesByConditions(@PathVariable String templateId,
                                                     @RequestParam(defaultValue = "ch") String lang,
                                                     @RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("conditions")) {
                return ResponseEntity.badRequest().body("缺少 conditions 参数");
            }

            Map<String, Object> raw = (Map<String, Object>) body.get("conditions");
            Map<String, String> conditions = raw.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().toString()
                    ));

            List<PriceRule> rules = priceRuleService.findRulesByConditions(templateId, lang, conditions);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("查询失败: " + e.getMessage());
        }
    }



}
