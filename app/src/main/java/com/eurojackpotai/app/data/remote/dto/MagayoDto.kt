package com.eurojackpotai.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response shapes for the magayo Lottery Data API (https://www.magayo.com/lottery-feeds/lottery-data-api/).
 * Field names mirror the documented API exactly so `kotlinx.serialization` can map them
 * without extra annotations. `error == 0` means success across all three endpoints.
 */

@Serializable
data class DrawResultsResponse(
    val error: Int,
    val draw: String? = null,
    // Comma-separated numbers. For EuroJackpot: 5 main numbers followed by 2 Euro numbers.
    val results: String? = null
)

@Serializable
data class NextJackpotResponse(
    val error: Int,
    val next_draw: String? = null,
    val currency: String? = null,
    val jackpot: String? = null
)

@Serializable
data class PastJackpotResponse(
    val error: Int,
    val currency: String? = null,
    val jackpot: String? = null
)
