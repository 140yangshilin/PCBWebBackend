package com.pcb.pcborderbackend.controller;

import com.pcb.pcborderbackend.service.CombinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/combinations")
public class CombinationController {

    @Autowired
    private CombinationService combinationService;


    @PostMapping("/generate")
    public ResponseEntity<?> generateCombinations(@RequestBody Map<String, Object> request) {
        try {
            combinationService.generateAndSaveCombinationsById(request);
            return ResponseEntity.ok("组合生成成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("生成失败：" + e.getMessage());
        }
    }



    @PostMapping("/getById")
    public ResponseEntity<?> getCombinationById(@RequestBody Map<String, String> request) {
        String templateId = request.get("template_id");
        String id = request.get("id");
        try {
            Map<String, Object> rule = combinationService.getCombinationById(templateId, id);
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("查询失败: " + e.getMessage());
        }
    }



    @PostMapping("/query")
    public ResponseEntity<?> queryCombinations(@RequestBody Map<String, Object> request,
                                               @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        try {
            return ResponseEntity.ok(combinationService.queryValidOptionsById(request, lang));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("查询失败: " + e.getMessage());
        }
    }




    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCombination(@RequestBody Map<String, Object> request) {
        try {
            combinationService.deleteCombinationByFields(request);
            return ResponseEntity.ok("组合规则已删除（中英文）");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }



}
