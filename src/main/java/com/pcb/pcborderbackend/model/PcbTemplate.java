package com.pcb.pcborderbackend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("pcb_templates")
@Data
public class PcbTemplate {
    @Id
    private String id;

    private String name; // 模板名称
    private List<Category> categories;

    private LocalDateTime createdAt;

    @Data
    public static class Category {
        private List<Field> fields;
    }

    @Data
    public static class Field {
        private String key;
        private String des;
        private String pic;
        private String type;
        private List<Option> options;
        private String defaultValue;
        private Integer order;
    }

    @Data
    public static class Option {
        private String name;
        private String des;
        private String pic;
    }
}
