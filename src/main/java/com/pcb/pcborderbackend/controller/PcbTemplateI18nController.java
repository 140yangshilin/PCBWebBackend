package com.pcb.pcborderbackend.controller;

import com.pcb.pcborderbackend.model.PcbTemplateI18n;
import com.pcb.pcborderbackend.service.PcbTemplateI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template-i18n")
public class PcbTemplateI18nController {

    @Autowired
    private PcbTemplateI18nService i18nService;

    // 提交翻译后的中英结构模板
    @PostMapping("/submit")
    public ResponseEntity<?> submitI18nTemplate(@RequestBody PcbTemplateI18n template) {
        try {
            return ResponseEntity.ok(i18nService.save(template));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 查询某个模板的中英文翻译结构
    @GetMapping("/{id}")
    public ResponseEntity<?> getI18nTemplate(@PathVariable String id) {
        return i18nService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 获取所有中英文原始结构
    @GetMapping("/all")
    public ResponseEntity<?> getAllI18nTemplates() {
        return ResponseEntity.ok(i18nService.getAllI18nTemplates());
    }


}
