package events.boudicca.search.model

import java.time.OffsetDateTime

data class SearchDTO(
    val name: String?,
    val fromDate: OffsetDateTime?,
    val toDate: OffsetDateTime?,
    val category: String? = null,
    val locationName: String? = null,
    val locationCity: String? = null,
    val offset: Int? = null,
    val size: Int? = null,
)