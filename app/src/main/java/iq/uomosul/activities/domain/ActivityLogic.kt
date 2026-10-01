package iq.uomosul.activities.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class College(val name: String, val website: String)

data class ActivityItem(
    val id: Long,
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val publishedAt: LocalDate?,
    val collegeName: String?,
)

data class RawPost(
    val id: Long,
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val date: String,
    val categoryIds: Set<Int>,
)

object ActivityLogic {
    private const val COLLEGE_ACTIVITIES_CATEGORY = 33

    private val activityTerms = listOf(
        "نشاط", "فعالية", "فعاليات", "ندوة", "ورشة", "محاضرة", "مؤتمر",
        "مهرجان", "احتفالية", "حملة", "مبادرة", "معرض", "دورة", "مسابقة",
        "احتفال", "مناقشة", "زيارة",
    )

    fun normalizeArabic(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun collegeFor(text: String, colleges: List<College>): College? {
        val normalizedText = " ${normalizeArabic(text)} "
        return colleges
            .sortedByDescending { normalizeArabic(it.name).length }
            .firstOrNull { college ->
                val name = normalizeArabic(college.name)
                name.isNotEmpty() && normalizedText.contains(" $name ")
            }
    }

    fun parsePublishedDate(value: String): LocalDate? {
        val normalizedDigits = value.map { character ->
            when (character) {
                in '\u0660'..'\u0669' -> '0' + (character - '\u0660')
                in '\u06f0'..'\u06f9' -> '0' + (character - '\u06f0')
                else -> character
            }
        }.joinToString("")

        return runCatching {
            LocalDateTime.parse(normalizedDigits, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
        }.getOrElse {
            runCatching {
                OffsetDateTime.parse(normalizedDigits, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
            }.getOrNull()
        }
    }

    fun isActivity(post: RawPost, college: College?): Boolean {
        if (COLLEGE_ACTIVITIES_CATEGORY in post.categoryIds) return true
        if (college == null) return false
        val content = normalizeArabic("${post.title} ${post.summary}")
        return activityTerms.any { normalizeArabic(it) in content }
    }

    fun toActivity(post: RawPost, colleges: List<College>): ActivityItem? {
        val college = collegeFor("${post.title} ${post.summary}", colleges)
        if (!isActivity(post, college)) return null
        return ActivityItem(
            id = post.id,
            title = post.title,
            summary = post.summary,
            sourceUrl = post.sourceUrl,
            publishedAt = parsePublishedDate(post.date),
            collegeName = college?.name,
        )
    }

    fun filter(
        activities: List<ActivityItem>,
        month: YearMonth?,
        collegeName: String?,
    ): List<ActivityItem> = activities.filter { activity ->
        (month == null || activity.publishedAt?.let(YearMonth::from) == month) &&
            (collegeName == null || activity.collegeName == collegeName)
    }
}
