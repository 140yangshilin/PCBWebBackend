package com.pcb.pcborderbackend.service;

import com.pcb.pcborderbackend.model.PcbTemplate;
import com.pcb.pcborderbackend.repository.PcbTemplateI18nRepository;
import com.pcb.pcborderbackend.repository.PcbTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PcbTemplateService {

    @Autowired
    private PcbTemplateRepository repository;
    @Autowired
    private PcbTemplateI18nRepository i18nRepository;

    // 创建
    public PcbTemplate create(PcbTemplate template) {
        if (repository.existsByName(template.getName())) {
            throw new RuntimeException("模版已存在");
        }

        // 自动生成 ID（如果前端未传）
        if (template.getId() == null || template.getId().isEmpty()) {
            template.setId(UUID.randomUUID().toString());
        }

        // 设置创建时间
        template.setCreatedAt(LocalDateTime.now());

        // 遍历每个 category 下的 field 列表，补全 options 和 order
        if (template.getCategories() != null) {
            for (PcbTemplate.Category category : template.getCategories()) {
                if (category.getFields() != null) {
                    int index = 0;
                    for (PcbTemplate.Field field : category.getFields()) {
                        // 没传 options 时补成空数组
                        if (field.getOptions() == null) {
                            field.setOptions(new ArrayList<>());
                        }

                        // 没传排序字段，自动生成顺序
                        if (field.getOrder() == null) {
                            field.setOrder(index++);
                        }
                    }
                }
            }
        }

        return repository.save(template);
    }

    public List<PcbTemplate> getAll() {
        return repository.findAll();
    }

    public Optional<PcbTemplate> getById(String id) {
        return repository.findById(id);
    }

    public Optional<PcbTemplate> updateTemplateByIdOrName(PcbTemplate updated) {
        Optional<PcbTemplate> optionalTemplate;

        // 先用 id 查找
        if (updated.getId() != null && !updated.getId().isEmpty()) {
            optionalTemplate = repository.findById(updated.getId());
        }
        // 如果没有 id，用 name 查
        else if (updated.getName() != null && !updated.getName().isEmpty()) {
            optionalTemplate = repository.findAll().stream()
                    .filter(t -> updated.getName().equals(t.getName()))
                    .findFirst();
        } else {
            return Optional.empty(); // 什么都没传
        }

        if (optionalTemplate.isEmpty()) return Optional.empty();

        PcbTemplate original = optionalTemplate.get();

        // 用 updated 覆盖原数据，但保留 ID 和 createdAt
        updated.setId(original.getId());
        updated.setCreatedAt(original.getCreatedAt());

        return Optional.of(repository.save(updated));
    }

    public boolean deleteById(String id) {
        boolean exists = repository.existsById(id);
        boolean i18nExists = i18nRepository.existsById(id);

        if (!exists && !i18nExists) {
            return false;
        }

        if (exists) {
            repository.deleteById(id);
        }
        if (i18nExists) {
            i18nRepository.deleteById(id);
        }

        return true;
    }


}
