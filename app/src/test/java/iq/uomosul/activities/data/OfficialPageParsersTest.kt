package iq.uomosul.activities.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialPageParsersTest {
    @Test
    fun extractsNamedCollegesAndOfficialCollegeWebsites() {
        val html = """
            <div class="fusion-layout-column">
              <h3>كلية الطب</h3>
              <a href="https://example.com/share/">مشاركة</a>
              <a href="https://uomosul.edu.iq/medicine/">الموقع الالكتروني</a>
            </div>
            <div class="fusion-layout-column">
              <h3>كلية الآداب</h3>
              <a href="https://arts.uomosul.edu.iq/">الموقع الالكتروني</a>
            </div>
            <div class="fusion-layout-column">
              <h3>كلية الصيدلة</h3>
              <a href="http://pharmacy.uomosul.edu.iq/">الموقع الالكتروني</a>
            </div>
        """.trimIndent()

        assertEquals(
            listOf(
                "كلية الطب" to "https://uomosul.edu.iq/medicine/",
                "كلية الآداب" to "https://arts.uomosul.edu.iq/",
            ),
            OfficialPageParsers.colleges(html).map { it.name to it.website },
        )
    }
}
