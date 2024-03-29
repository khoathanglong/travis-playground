package org.radarbase.upload

import org.radarbase.upload.inject.ManagementPortalResources
import java.net.URI

data class Config(
        var baseUri: URI = URI.create("http://0.0.0.0:8080/radar-upload/"),
        var managementPortalUrl: String = "http://managementportal-app:8080/managementportal/",
        var resourceConfig: Class<*> = ManagementPortalResources::class.java,
        var jdbcDriver: String? = "org.h2.Driver",
        var jdbcUrl: String? = null,
        var jdbcUser: String? = null,
        var jdbcPassword: String? = null,
        var jwtECPublicKeys: List<String>? = null,
        var jwtRSAPublicKeys: List<String>? = null,
        var jwtIssuer: String? = null,
        var jwtResourceName: String = "res_upload")
