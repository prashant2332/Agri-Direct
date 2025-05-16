package com.example.finalyearprojectwithfirebase.model


data class MandiResponse(
    val created: Long,
    val updated: Long,
    val created_date: String,
    val updated_date: String,
    val active: String,
    val index_name: String,
    val org: List<String>,
    val org_type: String,
    val source: String,
    val title: String,
    val external_ws_url: String?,
    val visualizable: Int,
    val field: List<Field>,
    val external_ws: Int,
    val catalog_uuid: String,
    val sector: List<String>,
    val target_bucket: TargetBucket,
    val desc: String,
    val field_exposed: List<Field>,
    val message: String,
    val version: String,
    val status: String,
    val total: Int,
    val count: Int,
    val limit: Int,
    val offset: Int,
    val records: List<MandiRecord>
)

data class Field(
    val name: String,
    val id: String,
    val type: String
)

data class TargetBucket(
    val field: String,
    val index: String,
    val type: String
)
