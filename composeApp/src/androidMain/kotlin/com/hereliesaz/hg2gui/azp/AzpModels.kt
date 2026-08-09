package com.hereliesaz.hg2gui.azp

import kotlinx.serialization.Serializable

/**
 * Wire shapes for the azphalt Repository API (spec/repository-api.md), trimmed to the fields
 * HG2Gui actually reads. Extra server fields are ignored by kotlinx.serialization's default
 * lenient decoding.
 */
@Serializable
data class AzpPackageSummary(
    val id: String,
    val name: String,
    val description: String = "",
    val author: String = "",
    val version: String = "",
    val kind: String = "asset",
    val priceStatus: String = "free",
    val downloads: Long = 0,
    val byteSize: Long = 0,
)

@Serializable
data class AzpSearchResponse(
    val packages: List<AzpPackageSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
)

/** A locally installed package, as tracked by [com.hereliesaz.hg2gui.managers.AzpLibrary]. */
data class AzpInstalled(
    val id: String,
    val name: String,
    val version: String,
    val kind: String,
)
