package com.pcb.pcborderbackend.service;

import com.pcb.pcborderbackend.model.PcbTemplate;
import com.pcb.pcborderbackend.model.PcbTemplateI18n;
import com.pcb.pcborderbackend.repository.PcbTemplateI18nRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PcbTemplateI18nService {

    @Autowired
    private PcbTemplateI18nRepository repository;

    public PcbTemplateI18n save(PcbTemplateI18n template) {
        if (template.getId() == null || template.getId().isEmpty()) {
            throw new RuntimeException("必须指定模板 ID");
        }

        Optional<PcbTemplateI18n> existing = repository.findById(template.getId());
        if (existing.isPresent()) {
            PcbTemplateI18n old = existing.get();
            template.setCreatedAt(old.getCreatedAt()); // 保留原创建时间
        } else {
            template.setCreatedAt(LocalDateTime.now());
        }

        return repository.save(template);
    }


    public Optional<PcbTemplateI18n> getById(String id) {
        return repository.findById(id);
    }

    public Optional<PcbTemplate> getConvertedEnglishTemplate(String id) {
        Optional<PcbTemplateI18n> source = repository.findById(id);
        if (source.isEmpty()) return Optional.empty();

        PcbTemplateI18n i18n = source.get();
        PcbTemplate result = new PcbTemplate();
        result.setId(i18n.getId());
        result.setName(i18n.getNameEn());
        result.setCreatedAt(i18n.getCreatedAt());

        List<PcbTemplate.Category> categories = new ArrayList<>();
        for (PcbTemplateI18n.Category cat : i18n.getCategories()) {
            PcbTemplate.Category newCat = new PcbTemplate.Category();
            List<PcbTemplate.Field> fields = new ArrayList<>();
            for (PcbTemplateI18n.Field f : cat.getFields()) {
                PcbTemplate.Field field = new PcbTemplate.Field();
                field.setKey(f.getKeyEn());
                field.setDes(f.getDesEn());
                field.setPic(f.getPic());
                field.setType(f.getType());
                field.setDefaultValue(f.getDefaultValue());
                field.setOrder(f.getOrder());

                List<PcbTemplate.Option> options = new ArrayList<>();
                for (PcbTemplateI18n.Option opt : f.getOptions()) {
                    PcbTemplate.Option o = new PcbTemplate.Option();
                    o.setName(opt.getNameEn());
                    o.setDes(opt.getDesEn());
                    o.setPic(opt.getPic());
                    options.add(o);
                }
                field.setOptions(options);
                fields.add(field);
            }
            newCat.setFields(fields);
            categories.add(newCat);
        }
        result.setCategories(categories);
        return Optional.of(result);
    }

    public List<PcbTemplateI18n> getAllI18nTemplates() {
        return repository.findAll();
    }

    public List<PcbTemplate> getAllConvertedEnglishTemplates() {
        List<PcbTemplateI18n> sourceList = repository.findAll();
        List<PcbTemplate> resultList = new ArrayList<>();

        for (PcbTemplateI18n i18n : sourceList) {
            PcbTemplate result = new PcbTemplate();
            result.setId(i18n.getId());
            result.setName(i18n.getNameEn());
            result.setCreatedAt(i18n.getCreatedAt());

            List<PcbTemplate.Category> categories = new ArrayList<>();
            for (PcbTemplateI18n.Category cat : i18n.getCategories()) {
                PcbTemplate.Category newCat = new PcbTemplate.Category();
                List<PcbTemplate.Field> fields = new ArrayList<>();
                for (PcbTemplateI18n.Field f : cat.getFields()) {
                    PcbTemplate.Field field = new PcbTemplate.Field();
                    field.setKey(f.getKeyEn());
                    field.setDes(f.getDesEn());
                    field.setPic(f.getPic());
                    field.setType(f.getType());
                    field.setDefaultValue(f.getDefaultValue());
                    field.setOrder(f.getOrder());

                    List<PcbTemplate.Option> options = new ArrayList<>();
                    for (PcbTemplateI18n.Option opt : f.getOptions()) {
                        PcbTemplate.Option o = new PcbTemplate.Option();
                        o.setName(opt.getNameEn());
                        o.setDes(opt.getDesEn());
                        o.setPic(opt.getPic());
                        options.add(o);
                    }
                    field.setOptions(options);
                    fields.add(field);
                }
                newCat.setFields(fields);
                categories.add(newCat);
            }
            result.setCategories(categories);
            resultList.add(result);
        }

        return resultList;
    }

}
