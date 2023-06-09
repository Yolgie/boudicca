package events.boudicca.eventcollector.collectors

import events.boudicca.SemanticKeys
import events.boudicca.api.eventcollector.Event
import events.boudicca.api.eventcollector.Fetcher
import events.boudicca.api.eventcollector.TwoStepEventCollector
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WissensturmCollector : TwoStepEventCollector<Pair<String, Document>>("wissensturm") {

    override fun getAllUnparsedEvents(): List<Pair<String, Document>> {
        val fetcher = Fetcher(500)

        val eventUrls = mutableListOf<String>()
        var date = LocalDate.now(ZoneId.of("CET"))

        //only collect 6 months for now
        for (i in 1..6) {
            val monthlyOverview =
                Jsoup.parse(fetcher.fetchUrl("https://vhskurs.linz.at/index.php?kathaupt=109&blkeep=1&month=${date.monthValue}&year=${date.year}"))
            eventUrls.addAll(monthlyOverview.select("div.kurse_demn article a").eachAttr("href"))
            date = date.plusMonths(1)
        }

        return eventUrls
            .map { "https://vhskurs.linz.at/$it" }
            .map { Pair(it, Jsoup.parse(fetcher.fetchUrl(it))) }
    }

    override fun parseMultipleEvents(pair: Pair<String, Document>): List<Event> {
        val (url, event) = pair
        val data = mutableMapOf<String, String>()

        val name = event.select("div.kw-kurdetails h1").text()
        val datesAndLocations = parseDates(event)

        data[SemanticKeys.DESCRIPTION] = event.select("div.kw-kurdetails div.content-txt:nth-child(2)").text()
        data[SemanticKeys.URL] = url

        val pictureUrl = event.select("div.kw-kurdetails div.content-txt:nth-child(2) img")
        if (pictureUrl.isNotEmpty()) {
            data[SemanticKeys.PICTUREURL] = pictureUrl.attr("src")
        }

        return datesAndLocations.map {
            val dataCopy = data.toMutableMap()
            dataCopy[SemanticKeys.ENDDATE] = DateTimeFormatter.ISO_DATE_TIME.format(it.second)
            if (
                it.third.contains("wissensturm", ignoreCase = true)
                || it.third.contains("WT;", ignoreCase = false)
            ) {
                dataCopy[SemanticKeys.LOCATION_NAME] = "Wissensturm"
                dataCopy[SemanticKeys.LOCATION_URL] = "https://wissensturm.linz.at/"
                dataCopy[SemanticKeys.LOCATION_CITY] = "Linz"
            } else {
                dataCopy[SemanticKeys.LOCATION_NAME] = it.third
            }
            Event(name, it.first, dataCopy)
        }
    }

    private fun parseDates(event: Document): List<Triple<OffsetDateTime, OffsetDateTime, String>> {
        return event.select("table tbody tr")
            .toList()
            .map {
                val fullDateText = it.child(0).text()
                val date = fullDateText.substring(4, 14)
                val startTime = fullDateText.substring(15, 20)
                val endTime = fullDateText.substring(23, 28)

                val localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.uuuu"))
                val localStartTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("kk:mm"))
                val localEndTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("kk:mm"))

                Triple(
                    localDate.atTime(localStartTime)
                        .atZone(ZoneId.of("CET"))
                        .toOffsetDateTime(),
                    localDate.atTime(localEndTime)
                        .atZone(ZoneId.of("CET"))
                        .toOffsetDateTime(),
                    it.child(it.childrenSize() - 1).text()
                )
            }
    }

}
