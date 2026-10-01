package iq.uomosul.activities.data

import iq.uomosul.activities.domain.College
import iq.uomosul.activities.domain.RawPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class PostPage(val posts: List<RawPost>, val totalPages: Int)

class UniversityRepository {
    suspend fun getColleges(): List<College> = withContext(Dispatchers.IO) {
        val json = getJson("$API_ROOT/pages/$COLLEGES_PAGE?_fields=content")
        val html = JSONObject(json).getJSONObject("content").getString("rendered")
        OfficialPageParsers.colleges(html)
    }

    suspend fun getPosts(categoryId: Int, page: Int): PostPage = withContext(Dispatchers.IO) {
        val url = "$API_ROOT/posts?categories=$categoryId&per_page=$PAGE_SIZE&page=$page" +
            "&orderby=date&order=desc&_fields=id,date,link,title,excerpt,categories"
        val (body, totalPages) = getResponse(url)
        val array = JSONArray(body)
        val posts = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val categories = item.getJSONArray("categories")
            val categoryIds = (0 until categories.length()).map { categories.getInt(it) }.toSet()
            RawPost(
                id = item.getLong("id"),
                title = Jsoup.parse(item.getJSONObject("title").getString("rendered")).text(),
                summary = Jsoup.parse(item.getJSONObject("excerpt").getString("rendered")).text(),
                sourceUrl = item.getString("link"),
                date = item.getString("date"),
                categoryIds = categoryIds,
            )
        }
        PostPage(posts, totalPages)
    }

    private fun getJson(url: String): String = getResponse(url).first

    private fun getResponse(url: String): Pair<String, Int> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "MosulUniversityActivities/1.0")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Official site returned HTTP $status")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return body to (connection.getHeaderField("X-WP-TotalPages")?.toIntOrNull() ?: 1)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val API_ROOT = "https://uomosul.edu.iq/wp-json/wp/v2"
        const val COLLEGES_PAGE = 72353
        const val PAGE_SIZE = 50
        const val TIMEOUT_MILLIS = 15_000
    }
}
