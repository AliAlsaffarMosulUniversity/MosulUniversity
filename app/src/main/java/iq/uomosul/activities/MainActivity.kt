package iq.uomosul.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iq.uomosul.activities.domain.ActivityItem
import iq.uomosul.activities.ui.ActivitiesUiState
import iq.uomosul.activities.ui.ActivitiesViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink = Color(0xFF18352C)
private val DeepGreen = Color(0xFF145A43)
private val Mint = Color(0xFFE5F1E9)
private val Canvas = Color(0xFFF5F7F3)
private val Muted = Color(0xFF65736A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: ActivitiesViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
                        ActivitiesScreen(state, model::refresh, model::loadMore, model::selectMonth, model::selectCollege)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitiesScreen(
    state: ActivitiesUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMonthSelected: (YearMonth?) -> Unit,
    onCollegeSelected: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(onRefresh, state.isLoading || state.isLoadingMore)
        if (state.error != null) {
            MessagePanel(
                title = "تعذر جلب الأنشطة",
                message = state.error,
                action = "إعادة المحاولة",
                onAction = onRefresh,
            )
            return
        }
        if (state.isLoading && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DeepGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("جارٍ جلب المنشورات الرسمية…", color = Muted)
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { IntroCard(state.items.size) }
            if (state.warnings.isNotEmpty()) {
                item { WarningPanel(state.warnings.joinToString("\n")) }
            }
            item { FilterPanel(state, onMonthSelected, onCollegeSelected) }
            item {
                Text(
                    text = "المنشورات المطابقة  ·  ${state.visibleItems.size}",
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.visibleItems.isEmpty()) {
                item {
                    MessagePanel(
                        title = if (state.items.isEmpty()) "لا توجد أنشطة منشورة" else "لا توجد نتائج لهذا الاختيار",
                        message = if (state.items.isEmpty()) {
                            "لم يعثر التطبيق على منشورات تطابق معايير النشاط في المصادر الرسمية المتاحة. استخدم زر التحديث أو جرّب لاحقاً."
                        } else {
                            "لم نعثر على منشور رسمي لهذه الكلية أو لهذا الشهر ضمن الصفحات المحمّلة. غيّر عوامل التصفية أو حمّل المزيد."
                        },
                    )
                }
            } else {
                items(state.visibleItems, key = { it.id }) { activity -> ActivityCard(activity) }
            }
            if (state.hasMore) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !state.isLoadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isLoadingMore) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (state.isLoadingMore) "جارٍ تحميل المزيد…" else "تحميل منشورات أقدم")
                    }
                }
            }
            item {
                Text(
                    "المعلومات والتواريخ وروابط المصدر من الموقع الرسمي لجامعة الموصل.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit, isLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DeepGreen).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("جامعة الموصل", color = Color(0xFFBCE1C8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("أنشطة الكليات", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("كل ما تنشره الجامعة، في مكان واحد", color = Color(0xFFE0ECE4), fontSize = 13.sp)
        }
        OutlinedButton(onClick = onRefresh, enabled = !isLoading) {
            Text(if (isLoading) "…" else "تحديث", color = Color.White)
        }
    }
}

@Composable
private fun IntroCard(total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("م", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 23.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("نبض الجامعة", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("تصفّح الأنشطة المنشورة رسمياً حسب الشهر والكلية.", color = Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toString(), color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text("منشور", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: ActivitiesUiState,
    onMonthSelected: (YearMonth?) -> Unit,
    onCollegeSelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تصفية الأنشطة", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val months = state.items.mapNotNull { it.publishedAt?.let(YearMonth::from) }.distinct().sortedDescending()
            FilterDropdown(
                label = state.selectedMonth?.format(MONTH_FORMAT) ?: "كل الأشهر",
                allLabel = "كل الأشهر",
                options = months.map { it to it.format(MONTH_FORMAT) },
                onSelected = { onMonthSelected(it) },
                modifier = Modifier.weight(1f),
            )
            FilterDropdown(
                label = state.selectedCollege ?: "كل الكليات",
                allLabel = "كل الكليات",
                options = state.colleges.map { it.name to it.name }.sortedBy { it.second },
                onSelected = onCollegeSelected,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.selectedMonth != null || state.selectedCollege != null) {
            Text(
                "إزالة التصفية",
                color = DeepGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    onMonthSelected(null)
                    onCollegeSelected(null)
                },
            )
        }
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    allLabel: String,
    options: List<Pair<T?, String>>,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: ActivityItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activity.sourceUrl)))
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    activity.collegeName ?: "جامعة الموصل",
                    color = DeepGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Mint, RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 6.dp),
                )
                Text(activity.publishedAt?.format(DATE_FORMAT) ?: "تاريخ النشر غير متاح", color = Muted, fontSize = 12.sp)
            }
            Text(activity.title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            if (activity.summary.isNotBlank()) {
                Text(activity.summary, color = Muted, fontSize = 13.sp, lineHeight = 21.sp, maxLines = 4)
            }
            Text("اقرأ المنشور في الموقع الرسمي  ↗", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WarningPanel(message: String) {
    Text(
        message,
        color = Color(0xFF795C19),
        fontSize = 13.sp,
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF4D8), RoundedCornerShape(14.dp)).padding(14.dp),
    )
}

@Composable
private fun MessagePanel(
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, color = Muted, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

private val IRAQI_ARABIC = Locale.forLanguageTag("ar-IQ")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", IRAQI_ARABIC)
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", IRAQI_ARABIC)
