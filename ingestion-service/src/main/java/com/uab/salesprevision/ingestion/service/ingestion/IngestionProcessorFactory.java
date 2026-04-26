package com.uab.salesprevision.ingestion.service.ingestion;

import com.uab.core.enums.FileType;
import com.uab.core.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IngestionProcessorFactory {

    private final List<IngestionProcessor> processors;

    public IngestionProcessorFactory(List<IngestionProcessor> processors) {
        this.processors = processors;
    }

    public IngestionProcessor getProcessor(FileType fileType) {
        return processors.stream()
                .filter(processor -> processor.supports() == fileType)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Não existe processador configurado para o tipo de ficheiro " + fileType
                ));
    }
}
