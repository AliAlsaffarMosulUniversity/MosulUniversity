package iq.uomosul.activities.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iq.uomosul.activities.data.UniversityRepository
import iq.uomosul.activities.domain.ActivityItem
import iq.uomosul.activities.domain.ActivityLogic
import iq.uomosul.activities.domain.College
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.time.YearMonth

data class ActivitiesUiState(
    val items: List<ActivityItem> = emptyList(),
    val colleges: List<College> = emptyList(),
    val selectedMonth: YearMonth? = null,
    val selectedCollege: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val nextNewsPage: Int = 1,
    val totalNewsPages: Int = 1,
    val nextActivitiesPage: Int = 1,
    val totalActivitiesPages: Int = 1,
) {
    val hasMore: Boolean
        get() = nextNewsPage <= totalNewsPages || nextActivitiesPage <= totalActivitiesPages

    val visibleItems: List<ActivityItem>
        get() = ActivityLogic.filter(items, selectedMonth, selectedCollege)
}

class ActivitiesViewModel(
    private val repository: UniversityRepository = UniversityRepository(),
) : ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(ActivitiesUiState())
    val state: kotlinx.coroutines.flow.StateFlow<ActivitiesUiState> = _state

    init {
        viewModelScope.launch {
            loadFirstPage()
        }
    }

    fun refresh() {
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        _state.value = _state.value.copy(isLoading = true, error = null, warnings = emptyList())
        viewModelScope.launch {
            loadFirstPage()
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            loadNextPages()
        }
    }

    fun selectMonth(month: YearMonth?) {
        _state.value = _state.value.copy(selectedMonth = month)
    }

    fun selectCollege(collegeName: String?) {
        _state.value = _state.value.copy(selectedCollege = collegeName)
    }

    private suspend fun loadFirstPage() {
        val warnings = mutableListOf<String>()
        val colleges = try {
            repository.getColleges()
        } catch (error: IOException) {
            warnings += "تعذر تحميل قائمة الكليات الرسمية."
            emptyList()
        } catch (error: JSONException) {
            warnings += "تعذر قراءة قائمة الكليات من الموقع الرسمي."
            emptyList()
        }

        val news = fetchSource(32, 1, warnings, "الأخبار الجامعية")
        val activities = fetchSource(33, 1, warnings, "نشاطات الكليات")
        if (news == null && activities == null) {
            _state.value = _state.value.copy(
                colleges = colleges,
                isLoading = false,
                error = "تعذر الاتصال بالموقع الرسمي. تحقق من الإنترنت ثم أعد المحاولة.",
                warnings = warnings,
            )
            return
        }

        val items = classify(news?.posts.orEmpty() + activities?.posts.orEmpty(), colleges)
        _state.value = _state.value.copy(
            items = items,
            colleges = colleges,
            isLoading = false,
            error = null,
            warnings = warnings,
            nextNewsPage = 2,
            totalNewsPages = news?.totalPages ?: 1,
            nextActivitiesPage = 2,
            totalActivitiesPages = activities?.totalPages ?: 1,
        )
    }

    private suspend fun loadNextPages() {
        val current = _state.value
        val warnings = current.warnings.toMutableList()
        val pages = mutableListOf<iq.uomosul.activities.data.PostPage>()
        var nextNewsPage = current.nextNewsPage
        var nextActivitiesPage = current.nextActivitiesPage
        var totalNewsPages = current.totalNewsPages
        var totalActivitiesPages = current.totalActivitiesPages
        if (nextNewsPage <= current.totalNewsPages) {
            fetchSource(32, nextNewsPage, warnings, "الأخبار الجامعية")?.let {
                pages += it
                nextNewsPage++
                totalNewsPages = it.totalPages
            }
        }
        if (nextActivitiesPage <= current.totalActivitiesPages) {
            fetchSource(33, nextActivitiesPage, warnings, "نشاطات الكليات")?.let {
                pages += it
                nextActivitiesPage++
                totalActivitiesPages = it.totalPages
            }
        }

        val newItems = classify(pages.flatMap { it.posts }, current.colleges)
        _state.value = current.copy(
            items = (current.items + newItems).distinctBy { it.id }.sortedByDescending { it.publishedAt },
            isLoadingMore = false,
            warnings = warnings.distinct(),
            nextNewsPage = nextNewsPage,
            nextActivitiesPage = nextActivitiesPage,
            totalNewsPages = totalNewsPages,
            totalActivitiesPages = totalActivitiesPages,
        )
    }

    private suspend fun fetchSource(
        categoryId: Int,
        page: Int,
        warnings: MutableList<String>,
        sourceName: String,
    ): iq.uomosul.activities.data.PostPage? = try {
        repository.getPosts(categoryId, page)
    } catch (error: IOException) {
        warnings += "تعذر تحميل $sourceName من المصدر الرسمي."
        null
    } catch (error: JSONException) {
        warnings += "تعذر قراءة $sourceName من المصدر الرسمي."
        null
    }

    private fun classify(
        posts: List<iq.uomosul.activities.domain.RawPost>,
        colleges: List<College>,
    ): List<ActivityItem> = posts.mapNotNull { ActivityLogic.toActivity(it, colleges) }
        .distinctBy { it.id }
        .sortedByDescending { it.publishedAt }
}
