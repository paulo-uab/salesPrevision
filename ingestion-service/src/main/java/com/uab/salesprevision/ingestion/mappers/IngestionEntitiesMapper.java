package com.uab.salesprevision.ingestion.mappers;

import com.uab.core.dto.ingestion.CreateIngestionJobResponse;
import com.uab.core.dto.ingestion.IngestedRecordResponse;
import com.uab.core.dto.ingestion.IngestionErrorResponse;
import com.uab.core.dto.ingestion.IngestionJobResponse;
import com.uab.salesprevision.ingestion.entity.IngestedRecord;
import com.uab.salesprevision.ingestion.entity.IngestionError;
import com.uab.salesprevision.ingestion.entity.IngestionJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface IngestionEntitiesMapper {

    @Mapping(target = "jobId", source = "id")
    CreateIngestionJobResponse ingestionJobToCreateIngestionJobResponse(IngestionJob job);

    IngestionJobResponse ingestionJobToIngestionJobResponse(IngestionJob job);

    @Mapping(target = "ingestionJobId", source = "ingestionJob.id")
    IngestedRecordResponse ingestedRecordToIngestedRecordResponse(IngestedRecord record);

    @Mapping(target = "ingestionJobId", source = "ingestionJob.id")
    @Mapping(target = "recordId", source = "record.id")
    IngestionErrorResponse ingestionErrorToIngestionErrorResponse(IngestionError error);
}
