package com.pcb.pcborderbackend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("pcb_templates_i18n")
@Data
public class PcbTemplateI18n {
    @Id
    private String id;

    private String nameZh;
    private String nameEn;
    private List<Category> categories;
    private LocalDateTime createdAt;

    @Data
    public static class Category {
        private List<Field> fields;
    }

    @Data
    public static class Field {
        private String keyZh;
        private String keyEn;
        private String desZh;
        private String desEn;
        private String pic;
        private String type;
        private List<Option> options;
        private String defaultValue;
        private Integer order;
    }

    @Data
    public static class Option {
        private String nameZh;
        private String nameEn;
        private String desZh;
        private String desEn;
        private String pic;
    }
}
