package org.radarbase.connect.upload.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.*
import org.radarbase.connect.upload.auth.Authorizer
import org.radarbase.connect.upload.exception.BadGatewayException
import org.radarbase.connect.upload.exception.NotAuthorizedException
import org.slf4j.LoggerFactory
import java.io.Closeable

class UploadBackendClient(
        private val auth: Authorizer,
        private var httpClient: OkHttpClient,
        private var uploadBackendBaseUrl: String) : Closeable {

    init {
        httpClient = httpClient.newBuilder().authenticator { _, response ->
            response.request()
                    .newBuilder()
                    .header("Authorization", "Bearer ${auth.accessToken()}")
                    .build()
        }.build()

        if (!this.uploadBackendBaseUrl.endsWith("/")) {
            this.uploadBackendBaseUrl += "/"
        }
    }

    fun pollRecords(configuration: PollDTO): RecordContainerDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/poll"))
                .post(RequestBody.create(APPLICATION_JSON, configuration.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return mapper.readValue(response.body()?.string(), RecordContainerDTO::class.java)
    }

    fun requestConnectorConfig(name: String): SourceTypeDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("source-types/").plus(name))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return mapper.readValue(response.body()?.string(), SourceTypeDTO::class.java)
    }

    fun requestAllConnectors(): SourceTypeContainerDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("source-types"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return mapper.readValue(response.body()?.string(), SourceTypeContainerDTO::class.java)
    }

    fun retrieveFile(record: RecordDTO, fileName: String): ResponseBody? {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/${record.id}/contents/${fileName}"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return response.body()
    }

    fun updateStatus(recordId: Long, newStatus: RecordMetadataDTO): RecordMetadataDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/$recordId/metadata"))
                .post(RequestBody.create(APPLICATION_JSON, newStatus.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return mapper.readValue(response.body()?.string(), RecordMetadataDTO::class.java)
    }

    fun addLogs(recordId: Long, status: LogsDto): RecordMetadataDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/$recordId/logs"))
                .post(RequestBody.create(APPLICATION_JSON, status.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return mapper.readValue(response.body()?.charStream(), RecordMetadataDTO::class.java)
    }

    override fun close() {
    }

    private fun OkHttpClient.executeRequest(request: Request): Response {
        val response = this.newCall(request).execute()
        if (response.isSuccessful) {
            logger.info("Request to ${request.url()} is SUCCESSFUL")
            return response
        } else {
            logger.info("Request to ${request.url()} has FAILED with response-code ${response.code()}")
            when (response.code()) {
                401 -> throw NotAuthorizedException("access token is not provided or is invalid : ${response.message()}")
                403 -> throw NotAuthorizedException("access token is not authorized to perform this request")
            }
            throw BadGatewayException("Failed to make request to ${request.url()}: Error code ${response.code()}:  ${response.body()?.string()}")
        }

    }

    private fun Any.toJsonString(): String = mapper.writeValueAsString(this)

    companion object {
        private val logger = LoggerFactory.getLogger(UploadBackendClient::class.java)
        private val APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8")
        private var mapper: ObjectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

}