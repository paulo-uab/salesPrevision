package com.uab.salesprevision.ingestion.service.ingestion;

import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.salesprevision.ingestion.entity.IngestionJob;
import com.uab.core.enums.FileType;


public interface IngestionProcessor {

    FileType supports();

    void process(IngestionJob job, TemplateDto template);
}
