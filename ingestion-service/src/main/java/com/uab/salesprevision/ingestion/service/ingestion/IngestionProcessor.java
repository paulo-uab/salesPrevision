package com.uab.salesprevision.ingestion.service.ingestion;

import com.uab.salesprevision.ingestion.client.dto.TemplateClientDto;
import com.uab.salesprevision.ingestion.model.IngestionJob;
import com.uab.core.enums.FileType;


public interface IngestionProcessor {

    FileType supports();

    void process(IngestionJob job, TemplateClientDto template);
}
