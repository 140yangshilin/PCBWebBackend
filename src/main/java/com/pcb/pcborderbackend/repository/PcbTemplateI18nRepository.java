package com.pcb.pcborderbackend.repository;

import com.pcb.pcborderbackend.model.PcbTemplateI18n;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PcbTemplateI18nRepository extends MongoRepository<PcbTemplateI18n, String> {
}
