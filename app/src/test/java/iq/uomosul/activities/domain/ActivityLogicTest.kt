package iq.uomosul.activities.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ActivityLogicTest {
    private val colleges = listOf(
        College("كلية الطب", "https://medicine.uomosul.edu.iq/"),
        College("كلية طب الأسنان", "https://dentistry.uomosul.edu.iq/"),
    )

    @Test
    fun parsesWordPressDatesAndArabicDigits() {
        assertEquals(LocalDate.of(2025, 12, 11), ActivityLogic.parsePublishedDate("2025-12-11T19:00:09"))
        assertEquals(LocalDate.of(2025, 12, 11), ActivityLogic.parsePublishedDate("٢٠٢٥-١٢-١١T١٩:٠٠:٠٩"))
        assertEquals(LocalDate.of(2025, 12, 11), ActivityLogic.parsePublishedDate("2025-12-11T19:00:09+03:00"))
        assertNull(ActivityLogic.parsePublishedDate("not a date"))
    }

    @Test
    fun matchesLongestOfficialCollegeName() {
        assertEquals("كلية طب الأسنان", ActivityLogic.collegeFor("مبادرة في كلية طب الأسنان", colleges)?.name)
        assertEquals("كلية الطب", ActivityLogic.collegeFor("ندوة كلية الطب بجامعة الموصل", colleges)?.name)
        assertNull(ActivityLogic.collegeFor("اجتماع مجلس الجامعة", colleges))
    }

    @Test
    fun onlyIncludesCollegeNewsWhenItNamesAnActivity() {
        val genericNews = post(1, "اجتماع كلية الطب", setOf(32))
        val actualActivity = post(2, "ندوة علمية في كلية الطب", setOf(32))
        val explicitlyTagged = post(3, "خبر من كلية الطب", setOf(33))

        assertNull(ActivityLogic.toActivity(genericNews, colleges))
        assertEquals("كلية الطب", ActivityLogic.toActivity(actualActivity, colleges)?.collegeName)
        assertEquals("كلية الطب", ActivityLogic.toActivity(explicitlyTagged, colleges)?.collegeName)
    }

    @Test
    fun filtersByPublicationMonthAndCollegeTogether() {
        val items = listOf(
            activity(1, LocalDate.of(2025, 12, 4), "كلية الطب"),
            activity(2, LocalDate.of(2025, 12, 20), "كلية طب الأسنان"),
            activity(3, LocalDate.of(2026, 1, 2), "كلية الطب"),
        )

        assertEquals(
            listOf(1L),
            ActivityLogic.filter(items, YearMonth.of(2025, 12), "كلية الطب").map { it.id },
        )
        assertEquals(
            listOf(2L),
            ActivityLogic.filter(items, null, "كلية طب الأسنان").map { it.id },
        )
        assertFalse(ActivityLogic.filter(items, YearMonth.of(2024, 1), null).isNotEmpty())
    }

    private fun post(id: Long, title: String, categories: Set<Int>) = RawPost(
        id = id,
        title = title,
        summary = "",
        sourceUrl = "https://uomosul.edu.iq/$id/",
        date = "2025-12-11T12:00:00",
        categoryIds = categories,
    )

    private fun activity(id: Long, date: LocalDate, collegeName: String) = ActivityItem(
        id = id,
        title = "نشاط رسمي",
        summary = "",
        sourceUrl = "https://uomosul.edu.iq/$id/",
        publishedAt = date,
        collegeName = collegeName,
    )
}
