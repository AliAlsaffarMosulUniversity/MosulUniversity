package iq.uomosul.activities.data

import iq.uomosul.activities.domain.College
import iq.uomosul.activities.domain.ActivityLogic
import org.jsoup.Jsoup

object OfficialPageParsers {
    fun colleges(html: String): List<College> {
        val document = Jsoup.parse(html)
        return document.select(".fusion-layout-column").mapNotNull { column ->
            val name = column.selectFirst("h3, h2")?.text()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null

            val website = column.select("a[href]")
                .filter { ActivityLogic.normalizeArabic(it.text()).contains("الموقع الالكتروني") }
                .mapNotNull { anchor ->
                    anchor.attr("abs:href").takeIf(::isOfficialCollegeLink)
                }
                .firstOrNull()
                ?: return@mapNotNull null

            College(name, website)
        }.distinctBy { it.name }
    }

    private fun isOfficialCollegeLink(value: String): Boolean {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
        return uri.scheme == "https" && !uri.host.isNullOrBlank()
    }
}
