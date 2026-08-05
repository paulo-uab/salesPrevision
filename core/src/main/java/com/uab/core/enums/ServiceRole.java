package com.uab.core.enums;

/**
 * Per-service authority granted to a user. An "admin" is simply a user holding
 * every value here; there is no separate ADMIN/USER role.
 */
public enum ServiceRole {
    TEMPLATE_READ, TEMPLATE_EDIT, TEMPLATE_EXECUTE,
    INGESTION_READ, INGESTION_EDIT, INGESTION_EXECUTE,
    PIPELINE_READ, PIPELINE_EDIT, PIPELINE_EXECUTE,
    BATCH_READ, BATCH_EDIT, BATCH_EXECUTE,
    USER_READ, USER_EDIT
}
