package com.uab.salesprevision.ingestion.mappers;

import com.uab.salesprevision.ingestion.dto.CreateIngestionJobResponse;
import com.uab.salesprevision.ingestion.dto.IngestedRecordResponse;
import com.uab.salesprevision.ingestion.dto.IngestionErrorResponse;
import com.uab.salesprevision.ingestion.dto.IngestionJobResponse;
import com.uab.salesprevision.ingestion.model.IngestedRecord;
import com.uab.salesprevision.ingestion.model.IngestionError;
import com.uab.salesprevision.ingestion.model.IngestionJob;
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
