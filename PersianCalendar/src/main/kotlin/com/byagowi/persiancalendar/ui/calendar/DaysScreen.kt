package com.byagowi.persiancalendar.ui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.core.util.lruCache
import com.byagowi.persiancalendar.EN_DASH
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.CalendarEvent
import com.byagowi.persiancalendar.entities.Clock
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.entities.Language
import com.byagowi.persiancalendar.global.isTalkBackEnabled
import com.byagowi.persiancalendar.global.language
import com.byagowi.persiancalendar.global.mainCalendar
import com.byagowi.persiancalendar.ui.calendar.calendarpager.Month
import com.byagowi.persiancalendar.ui.calendar.calendarpager.MonthColors
import com.byagowi.persiancalendar.ui.calendar.calendarpager.PagerArrow
import com.byagowi.persiancalendar.ui.calendar.calendarpager.calendarPagerSize
import com.byagowi.persiancalendar.ui.calendar.calendarpager.pagerArrowSizeAndPadding
import com.byagowi.persiancalendar.ui.common.ExpandArrow
import com.byagowi.persiancalendar.ui.common.NavigationNavigateUpIcon
import com.byagowi.persiancalendar.ui.common.ScreenSurface
import com.byagowi.persiancalendar.ui.common.ScrollShadow
import com.byagowi.persiancalendar.ui.common.ShrinkingFloatingActionButton
import com.byagowi.persiancalendar.ui.common.TodayActionButton
import com.byagowi.persiancalendar.ui.resumeToken
import com.byagowi.persiancalendar.ui.theme.appMonthColors
import com.byagowi.persiancalendar.ui.theme.appTopAppBarColors
import com.byagowi.persiancalendar.ui.utils.AppBlendAlpha
import com.byagowi.persiancalendar.utils.applyWeekStartOffsetToWeekDay
import com.byagowi.persiancalendar.utils.dayTitleSummary
import com.byagowi.persiancalendar.utils.formatNumber
import com.byagowi.persiancalendar.utils.monthName
import com.byagowi.persiancalendar.utils.toCivilDate
import com.byagowi.persiancalendar.variants.debugAssertNotNull
import io.github.persiancalendar.calendar.AbstractDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DaysScreen(
    calendarViewModel: CalendarViewModel,
    initialSelectedDay: Jdn,
    animatedContentScope: AnimatedContentScope,
    navigateUp: () -> Unit,
    isInitiallyWeek: Boolean,
    navigateToSchedule: (Jdn) -> Unit,
) {
    var selectedDay by rememberSaveable(
        saver = Saver(save = { it.value.value }, restore = { mutableStateOf(Jdn(it)) })
    ) { mutableStateOf(initialSelectedDay) }
    val date = selectedDay.inCalendar(mainCalendar)
    val today by calendarViewModel.today.collectAsState()
    val context = LocalContext.current
    var isEverClicked by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val weekInitialPage = remember(today) { weekPageFromJdn(initialSelectedDay, today) }
    val weekPagerState = rememberPagerState(initialPage = weekInitialPage) { weeksLimit }
    val dayInitialPage = remember(today) { dayPageFromJdn(selectedDay, today) }
    val dayPagerState = rememberPagerState(initialPage = dayInitialPage) { daysLimit }
    val setSelectedDayInWeekPager = { jdn: Jdn ->
        selectedDay = jdn
        coroutineScope.launch {
            val destination = dayPageFromJdn(jdn, today)
            if (abs(destination - dayPagerState.currentPage) > 1) {
                dayPagerState.scrollToPage(destination)
            } else dayPagerState.animateScrollToPage(destination)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isEverClicked && calendarViewModel.selectedDay.value != selectedDay) {
                bringDate(calendarViewModel, selectedDay, context, highlight = selectedDay != today)
            }
        }
    }
    val addEvent = addEvent(calendarViewModel)
    val hasWeeksPager = LocalConfiguration.current.screenHeightDp > 600
    val language by language.collectAsState()
    var isAddEventBoxEnabled by remember { mutableStateOf(false) }

    val todayButtonAction = {
        isAddEventBoxEnabled = false
        selectedDay = today
        coroutineScope.launch {
            // Don't change their order before testing

            if (abs(weekPagerState.currentPage - weeksLimit / 2) == 1) {
                weekPagerState.animateScrollToPage(weeksLimit / 2)
            } else weekPagerState.scrollToPage(weeksLimit / 2)

            if (abs(dayPagerState.currentPage - daysLimit / 2) == 1) {
                dayPagerState.animateScrollToPage(daysLimit / 2)
            } else dayPagerState.scrollToPage(daysLimit / 2)
        }
    }

    var isFirstTime by remember { mutableStateOf(true) }
    LaunchedEffect(today) {
        if (isFirstTime) isFirstTime = false else if (selectedDay == today - 1) todayButtonAction()
    }

    var isWeekView by rememberSaveable { mutableStateOf(isInitiallyWeek) }
    var addAction by remember { mutableStateOf({}) }

    val snackbarHostState = remember { SnackbarHostState() }

    val swipeDownModifier = Modifier.pointerInput(Unit) {
        val threshold = 80.dp.toPx()
        awaitEachGesture {
            val id = awaitFirstDown(requireUnconsumed = false).id
            var successful = false
            var yAccumulation = 0f
            verticalDrag(id) {
                yAccumulation += it.positionChange().y
                if (!successful && yAccumulation > threshold) {
                    navigateUp()
                    successful = true
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ShrinkingFloatingActionButton(
                modifier = Modifier.padding(end = 8.dp),
                isVisible = true,
                action = addAction,
                icon = Icons.Default.Add,
                title = stringResource(R.string.add_event),
                noTitle = true,
            )
        },
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class) TopAppBar(
                modifier = swipeDownModifier,
                title = {
                    Column {
                        Crossfade(
                            if (hasWeeksPager) date.monthName
                            else language.dm.format(formatNumber(date.dayOfMonth), date.monthName),
                            label = "title",
                        ) { state -> Text(state, style = MaterialTheme.typography.titleLarge) }
                        Crossfade(formatNumber(date.year), label = "subtitle") { state ->
                            Text(state, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
                colors = appTopAppBarColors(),
                navigationIcon = { NavigationNavigateUpIcon(navigateUp) },
                actions = {
                    TodayActionButton(today != selectedDay || isAddEventBoxEnabled) {
                        todayButtonAction()
                    }
                    IconButton({
                        isWeekView = !isWeekView
                        // Ugly fix
                        val storedSelectedDay = selectedDay
                        if (!isWeekView) coroutineScope.launch {
                            delay(100)
                            selectedDay = storedSelectedDay
                            dayPagerState.scrollToPage(dayPageFromJdn(selectedDay, today))
                        }
                    }) {
                        val title = if (isWeekView) stringResource(R.string.day_view)
                        else stringResource(R.string.week_view)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(title) } },
                            state = rememberTooltipState()
                        ) {
                            Crossfade(isWeekView, label = "icon") { state ->
                                if (state) Icon(Icons.Default.CalendarViewDay, title)
                                else Icon(Icons.Default.CalendarViewWeek, title)
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val monthColors = appMonthColors()
        val bottomPadding = paddingValues.calculateBottomPadding().coerceAtLeast(16.dp)
        BoxWithConstraints(Modifier.padding(top = paddingValues.calculateTopPadding())) {
            val pagerSize =
                calendarPagerSize(false, this.maxWidth, this.maxHeight, bottomPadding, true)
            // Don't show weeks pager if there isn't enough space
            Column {
                val refreshToken by calendarViewModel.refreshToken.collectAsState()
                HorizontalPager(
                    state = weekPagerState,
                    verticalAlignment = Alignment.Top,
                    pageSpacing = 2.dp,
                ) { page ->
                    Column {
                        if (hasWeeksPager) Box(swipeDownModifier) {
                            WeekPager(
                                pagerSize = pagerSize,
                                addEvent = addEvent,
                                monthColors = monthColors,
                                selectedDay = selectedDay,
                                selectedDayDate = date,
                                setSelectedDay = { jdn -> setSelectedDayInWeekPager(jdn) },
                                setEverClicked = { isEverClicked = true },
                                animatedContentScope = animatedContentScope,
                                language = language,
                                coroutineScope = coroutineScope,
                                weekPagerState = weekPagerState,
                                page = page,
                                today = today,
                                refreshToken = refreshToken,
                            )
                        }
                        if (isWeekView) {
                            Spacer(Modifier.height(8.dp))
                            val weekStart = (today + (page - weeksLimit / 2) * 7).let {
                                it - applyWeekStartOffsetToWeekDay(it.weekDay)
                            }
                            val isInitialWeek = initialSelectedDay - weekStart in 0..<7
                            ScreenSurface(
                                animatedContentScope = animatedContentScope,
                                disableSharedContent = !isInitialWeek,
                            ) {
                                DaysView(
                                    bottomPadding = bottomPadding,
                                    setAddAction = {
                                        if (weekPagerState.currentPage == page) addAction = it
                                    },
                                    startingDay = weekStart,
                                    selectedDay = selectedDay,
                                    setSelectedDay = {
                                        isEverClicked = true
                                        setSelectedDayInWeekPager(it)
                                    },
                                    addEvent = addEvent,
                                    refreshCalendar = calendarViewModel::refreshCalendar,
                                    refreshToken = refreshToken,
                                    days = 7,
                                    now = calendarViewModel.now.collectAsState().value,
                                    navigateToSchedule = navigateToSchedule,
                                    isAddEventBoxEnabled = isAddEventBoxEnabled,
                                    setAddEventBoxEnabled = { isAddEventBoxEnabled = true },
                                    snackbarHostState = snackbarHostState,
                                )
                            }
                        }
                    }
                }
                if (!isWeekView) {
                    Spacer(Modifier.height(8.dp))
                    ScreenSurface(animatedContentScope) {
                        HorizontalPager(
                            state = dayPagerState,
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            val isCurrentPage = dayPagerState.currentPage == page
                            val pageDay = today + (page - daysLimit / 2)
                            LaunchedEffect(isCurrentPage) {
                                if (isCurrentPage) {
                                    selectedDay = pageDay
                                    val destination = weekPageFromJdn(pageDay, today)
                                    if (abs(destination - weekPagerState.currentPage) > 1) {
                                        weekPagerState.scrollToPage(destination)
                                    } else weekPagerState.animateScrollToPage(destination)
                                }
                            }

                            DaysView(
                                bottomPadding = bottomPadding,
                                setAddAction = {
                                    if (dayPagerState.currentPage == page) addAction = it
                                },
                                startingDay = pageDay,
                                selectedDay = selectedDay,
                                setSelectedDay = { selectedDay = it },
                                addEvent = addEvent,
                                refreshCalendar = calendarViewModel::refreshCalendar,
                                refreshToken = refreshToken,
                                days = 1,
                                now = calendarViewModel.now.collectAsState().value,
                                navigateToSchedule = navigateToSchedule,
                                isAddEventBoxEnabled = isAddEventBoxEnabled,
                                setAddEventBoxEnabled = { isAddEventBoxEnabled = true },
                                snackbarHostState = snackbarHostState,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun weekPageFromJdn(day: Jdn, today: Jdn): Int {
    val daysStart = day - applyWeekStartOffsetToWeekDay(day.weekDay)
    val todaysStart = today - applyWeekStartOffsetToWeekDay(today.weekDay)
    return (daysStart - todaysStart) / 7 + weeksLimit / 2
}

private fun dayPageFromJdn(day: Jdn, today: Jdn): Int = day - today + daysLimit / 2

private fun hoursFractionOfDay(date: GregorianCalendar): Float =
    date[Calendar.HOUR_OF_DAY] + date[Calendar.MINUTE] / 60f

private data class EventDivision(
    val event: CalendarEvent.DeviceCalendarEvent, val column: Int, val columnsCount: Int
)

private fun addDivisions(events: List<CalendarEvent.DeviceCalendarEvent>): List<EventDivision> {
    val graph = Graph(events.size)
    events.indices.forEach { i ->
        events.indices.forEach inner@{ j ->
            if (i == j) return@inner
            val a = events[i]
            val b = events[j]
            if (a.start.timeInMillis in (b.start.timeInMillis..<b.end.timeInMillis)) {
                graph.addEdge(i, j)
            }
        }
    }
    val columnsCount = MutableList(events.size) { 0 }
    val colors = graph.coloring()
    graph.connectedComponents().forEach { group ->
        val max = group.maxOf { colors[it] } + 1
        group.forEach { columnsCount[it] = max }
    }
    return events.indices.map { EventDivision(events[it], colors[it], columnsCount[it]) }
}

@Composable
private fun DaysView(
    setAddAction: (() -> Unit) -> Unit,
    startingDay: Jdn,
    selectedDay: Jdn,
    setSelectedDay: (Jdn) -> Unit,
    bottomPadding: Dp,
    addEvent: (AddEventData) -> Unit,
    refreshCalendar: () -> Unit,
    refreshToken: Int,
    now: Long,
    days: Int,
    navigateToSchedule: (Jdn) -> Unit,
    isAddEventBoxEnabled: Boolean,
    setAddEventBoxEnabled: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val scale = remember { Animatable(1f) }
    val cellHeight by remember(scale.value) { mutableStateOf((64 * scale.value).dp) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val initialScroll = with(density) { (cellHeight * 7 - 16.dp).roundToPx() }
    val scrollState = rememberScrollState(initialScroll)
    val events = (startingDay..(startingDay + days - 1)).toList()
        .map { jdn -> readEvents(jdn, refreshToken) }
    val eventsWithTime = events.map { dayEvents ->
        val deviceEvents = dayEvents.filterIsInstance<CalendarEvent.DeviceCalendarEvent>()
        addDivisions(deviceEvents.filter { it.time != null }.sortedWith { x, y ->
            x.start.timeInMillis.compareTo(y.end.timeInMillis).let {
                if (it != 0) return@sortedWith it
            }
            // If both start at the same time, put bigger events first, better for interval graphs
            y.start.timeInMillis.compareTo(x.end.timeInMillis)
        })
    }
    val eventsWithoutTime = events.map { dayEvents ->
        dayEvents.filter { it !is CalendarEvent.DeviceCalendarEvent || it.time == null }
    }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        coroutineScope.launch {
                            val value = scale.value * event.calculateZoom()
                            scale.snapTo(value.coerceIn(.5f, 2f))
                        }
                    } while (event.changes.fastAny { it.pressed })
                }
            },
    ) {
        val maxDayAllDayEvents = eventsWithoutTime.maxOf { it.size }
        val hasHeader by remember(events) {
            val needsHeader = eventsWithTime.all { it.isEmpty() } || maxDayAllDayEvents != 0
            derivedStateOf {
                needsHeader && !scrollState.lastScrolledForward && scrollState.value <= initialScroll * scale.value
            }
        }
        val launcher = rememberLauncherForActivityResult(ViewEventContract()) {
            refreshCalendar()
        }
        val context = LocalContext.current
        val defaultWidthReductionDp = 2.dp
        val defaultWidthReduction = with(density) { defaultWidthReductionDp.toPx() }
        AnimatedVisibility(hasHeader) {
            if (days == 1) Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                if (events[0].isEmpty()) Text(
                    stringResource(R.string.no_event),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                DayEvents(eventsWithoutTime[0].take(3)) { refreshCalendar() }
                if (eventsWithoutTime[0].size > 3) {
                    Spacer(Modifier.height(4.dp))
                    MoreButton(stringResource(R.string.more)) {
                        navigateToSchedule(selectedDay)
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else BoxWithConstraints {
                val cellWidth = (this.maxWidth - pagerArrowSizeAndPadding.dp * 2) / days
                var isExpanded by rememberSaveable { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = if (maxDayAllDayEvents > 3) Modifier.clickable(
                        onClickLabel = stringResource(R.string.more),
                        indication = null,
                        interactionSource = null,
                    ) { isExpanded = !isExpanded } else Modifier
                ) {
                    Box(
                        Modifier
                            .width(pagerArrowSizeAndPadding.dp)
                            .align(Alignment.Bottom),
                    ) {
                        if (maxDayAllDayEvents > 3) Box(
                            Modifier
                                .width(pagerArrowSizeAndPadding.dp)
                                .padding(bottom = 2.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) { ExpandArrow(isExpanded = isExpanded) }
                    }
                    Row(
                        Modifier
                            .padding(end = pagerArrowSizeAndPadding.dp)
                            .animateContentSize(),
                    ) {
                        eventsWithoutTime.forEach { dayEvents ->
                            Column(Modifier.weight(1f)) {
                                dayEvents.forEachIndexed { i, event ->
                                    if (isExpanded || i < 2 || (i == 2 && dayEvents.size == 3)) {
                                        val color = eventColor(event)
                                        Text(
                                            " " + event.title,
                                            maxLines = 1,
                                            fontSize = 12.sp,
                                            color = eventTextColor(color),
                                            modifier = Modifier
                                                .requiredWidth(cellWidth - defaultWidthReductionDp)
                                                .padding(
                                                    top = if (i == 0) 2.dp else 0.dp,
                                                    bottom = 2.dp,
                                                )
                                                .clip(MaterialTheme.shapes.small)
                                                .background(eventColor(event))
                                                .clickable {
                                                    if (event is CalendarEvent.DeviceCalendarEvent) {
                                                        launcher.viewEvent(event, context)
                                                    } else coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(event.title)
                                                    }
                                                },
                                        )
                                    }
                                    if (i == 2 && dayEvents.size > 3 && !isExpanded) Text(
                                        " +" + formatNumber(dayEvents.size - 3),
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        BoxWithConstraints {
            val firstColumnPx = with(density) { pagerArrowSizeAndPadding.dp.toPx() }
            val tableWidth = this@BoxWithConstraints.maxWidth - when (days) {
                1 -> pagerArrowSizeAndPadding.dp + 24.dp - defaultWidthReductionDp
                else -> pagerArrowSizeAndPadding.dp * 2
            }
            val oneDayTableWidthPx = with(density) { (tableWidth + 24.dp).toPx() }
            val tableWidthPx = with(density) { tableWidth.toPx() }
            val cellWidth = tableWidth / days
            val cellWidthPx = tableWidthPx / days
            val cellHeightPx = with(density) { cellHeight.toPx() }
            var offset by remember(tableWidthPx) { mutableStateOf(Offset.Unspecified) }
            var duration by remember { mutableFloatStateOf(cellHeightPx) }
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val directionSign = if (isRtl) -1 else 1
            val lineSize = with(density) { 1.dp.toPx() }
            val heightSizeReduction = 3.dp
            val heightSizeReductionPx = with(density) { heightSizeReduction.toPx() }

            Box(Modifier.verticalScroll(scrollState)) {
                val clockCache = remember {
                    lruCache(1024, create = { minutes: Int ->
                        Clock.fromMinutesCount(minutes).toBasicFormatString()
                    })
                }
                Box(Modifier.fillMaxWidth()) {
                    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(cellHeight * 24 + bottomPadding),
                    ) {
                        val paintCellWidthPx = if (days == 1) oneDayTableWidthPx else cellWidthPx
                        (0..days).forEach { i ->
                            val x = (firstColumnPx + paintCellWidthPx * i).let {
                                if (isRtl) this.size.width - it else it
                            }
                            val y = this.size.height
                            drawLine(outlineVariant, Offset(x, lineSize * 2), Offset(x, y))
                        }
                        val x1 = firstColumnPx.let { if (isRtl) this.size.width - it else it }
                        val x2 = (firstColumnPx + paintCellWidthPx * days).let {
                            if (isRtl) this.size.width - it else it
                        }
                        (0..23).forEach {
                            val y = if (it == 0) lineSize * 2 else cellHeightPx * it
                            drawLine(outlineVariant, Offset(x1, y), Offset(x2, y))
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        repeat(9) { column ->
                            Column {
                                if (column == 0) Spacer(Modifier.height(cellHeight / 2))
                                repeat(24) { row ->
                                    Box(
                                        when (column) {
                                            0, days + 1 -> Modifier
                                            else -> Modifier
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = null,
                                                ) {
                                                    offset = Offset(
                                                        cellWidthPx * (column - 1),
                                                        cellHeightPx * row / scale.value
                                                    )
                                                    setAddEventBoxEnabled()
                                                    duration = cellHeightPx / scale.value
                                                    setSelectedDay(startingDay + column - 1)
                                                }
                                                .then(if (isTalkBackEnabled) Modifier.semantics {
                                                    this.contentDescription =
                                                        (startingDay + (column - 1)).weekDayName + " " + clockCache[row * 60] + " " + clockCache[(row + 1) * 60]
                                                } else Modifier)
                                        }.size(
                                            if (column == 0 || column == days + 1) pagerArrowSizeAndPadding.dp
                                            else cellWidth,
                                            if (column == 0 && row == 23) 0.dp else cellHeight,
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (column == 0 && row != 23) {
                                            Text(clockCache[(row + 1) * 60], fontSize = 12.sp)
                                            HorizontalDivider(
                                                Modifier
                                                    .width(8.dp)
                                                    .align(Alignment.CenterEnd),
                                                Dp.Hairline,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                eventsWithTime.mapIndexed { i, it ->
                    it.map { (event, column, columnsCount) ->
                        val start = hoursFractionOfDay(event.start)
                        val end = hoursFractionOfDay(event.end)
                        val color = eventColor(event)
                        Text(
                            " " + event.title,
                            color = eventTextColor(color),
                            maxLines = 1,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (firstColumnPx + cellWidthPx * i + cellWidthPx / columnsCount * column).roundToInt(),
                                        (start * cellHeightPx).roundToInt()
                                    )
                                }
                                .requiredSize(
                                    with(density) { (cellWidthPx / columnsCount - defaultWidthReduction).toDp() },
                                    with(density) { ((end - start) * cellHeightPx).toDp() - heightSizeReduction },
                                )
                                .clickable { launcher.viewEvent(event, context) }
                                .background(color, MaterialTheme.shapes.small)
                        )
                    }
                }

                val x = if (offset == Offset.Unspecified) 0
                else (offset.x / cellWidthPx).roundToInt()
                LaunchedEffect(selectedDay) {
                    val selectedDayIndex = selectedDay - startingDay
                    if (offset != Offset.Unspecified && selectedDayIndex != x)
                        offset = offset.copy(x = selectedDayIndex * cellWidthPx)
                }
                val ySteps = (cellHeightPx / 4).roundToInt()
                val y = if (offset == Offset.Unspecified) 0
                else (offset.y * scale.value / ySteps).roundToInt()
                val animatedOffset = if (offset == Offset.Unspecified) Offset.Zero
                else animateOffsetAsState(
                    Offset(x * cellWidthPx, y * ySteps.toFloat()),
                    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                    label = "offset"
                ).value
                val dy = (duration / (cellHeightPx / 4) * scale.value).roundToInt()
                val animatedDuration by animateFloatAsState(
                    targetValue = dy * (cellHeightPx / 4),
                    label = "duration"
                )

                var resetOnNextRefresh by remember { mutableStateOf(false) }
                val addAction = {
                    val cellHeightScaledPx = cellHeightPx * scale.value
                    if (offset == Offset.Unspecified) {
                        offset = Offset(
                            cellWidthPx * (selectedDay - startingDay),
                            ceil(scrollState.value / cellHeightScaledPx) * cellHeightScaledPx
                        )
                        setAddEventBoxEnabled()
                    } else {
                        val time = selectedDay.toGregorianCalendar()
                        run {
                            val minutes = y * 15
                            time[GregorianCalendar.HOUR_OF_DAY] = minutes / 60
                            time[GregorianCalendar.MINUTE] = minutes % 60
                        }
                        time[GregorianCalendar.SECOND] = 0
                        val beginTime = time.time
                        run {
                            val minutes = (y + dy) * 15
                            time[GregorianCalendar.HOUR_OF_DAY] = minutes / 60
                            time[GregorianCalendar.MINUTE] = minutes % 60
                        }
                        val endTime = time.time
                        addEvent(
                            AddEventData(
                                beginTime = beginTime,
                                endTime = endTime,
                                allDay = false,
                                description = dayTitleSummary(
                                    selectedDay,
                                    selectedDay.inCalendar(mainCalendar),
                                ),
                            )
                        )
                        resetOnNextRefresh = true
                    }
                }
                setAddAction(addAction)

                val resumeToken by resumeToken.collectAsState()
                LaunchedEffect(resumeToken) {
                    if (resetOnNextRefresh) {
                        duration = cellHeightPx / 4 * 4f
                        offset = Offset.Unspecified
                        resetOnNextRefresh = false
                    }
                }

                LaunchedEffect(isAddEventBoxEnabled) {
                    if (!isAddEventBoxEnabled && offset != Offset.Unspecified) {
                        offset = Offset.Unspecified
                    }
                }

                val widthReduction = remember { Animatable(defaultWidthReduction) }
                val radius = with(density) { 4.dp.toPx() }
                // The same value as MaterialTheme.shapes.small
                val corner = with(density) { 8.dp.toPx() }

                // Time indicator
                val time = GregorianCalendar().also { it.timeInMillis = now }
                val offsetDay = Jdn(time.toCivilDate()) - startingDay
                val primary = MaterialTheme.colorScheme.primary
                if (offsetDay in 0..<days) Canvas(Modifier
                    .offset {
                        IntOffset(
                            (cellWidthPx * offsetDay + firstColumnPx).roundToInt(),
                            (hoursFractionOfDay(time) * cellHeightPx).roundToInt()
                        )
                    }
                    .size(1.dp)
                ) {
                    drawCircle(primary, radius)
                    drawLine(
                        color = primary,
                        start = Offset(if (isRtl) this.size.width else 0f, 0f),
                        end = Offset(
                            directionSign * if (days == 1) oneDayTableWidthPx else cellWidthPx,
                            0f
                        ),
                        strokeWidth = lineSize
                    )
                }

                var intention by remember { mutableStateOf<DragIntention?>(null) }

                // Add event box
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (animatedOffset.x + firstColumnPx).roundToInt(),
                                (animatedOffset.y + if (intention == DragIntention.ExtendUp) {
                                    duration * scale.value - animatedDuration
                                } else 0f).roundToInt(),
                            )
                        }
                        .size(
                            with(density) { cellWidthPx.toDp() - 1.dp },
                            with(density) {
                                (animatedDuration + if (intention == DragIntention.ExtendUp) {
                                    offset.y * scale.value - animatedOffset.y
                                } else 0f).toDp()
                            },
                        )
                        .clickable(
                            indication = null,
                            interactionSource = null,
                            onClickLabel = stringResource(R.string.add_event)
                        ) { addAction() }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val id = awaitFirstDown().id
                                coroutineScope.launch { widthReduction.animateTo(0f) }
                                drag(id) {
                                    if (offset == Offset.Unspecified) return@drag
                                    val delta = it.positionChange()
                                    if (intention == null) intention = when {
                                        abs(it.position.y - duration * scale.value) < cellHeightPx * scale.value * .2f -> DragIntention.ExtendDown

                                        abs(it.position.y) < cellHeightPx * scale.value * .2f -> DragIntention.ExtendUp
                                        else -> DragIntention.Move
                                    }
                                    when (intention) {
                                        DragIntention.ExtendDown -> duration =
                                            (duration + delta.y / scale.value).coerceIn(
                                                minimumValue = ySteps * 1f,
                                                maximumValue = (ySteps * 24 * 4) - offset.y
                                            )

                                        DragIntention.ExtendUp -> {
                                            val newValueY = offset.y + delta.y / scale.value
                                            offset = offset.copy(
                                                y = newValueY.coerceIn(0f, cellHeightPx * 23)
                                            )
                                            duration =
                                                (duration - delta.y / scale.value).coerceAtLeast(
                                                    ySteps * 1f
                                                )
                                        }

                                        DragIntention.Move -> {
                                            val newValueX = offset.x + directionSign * delta.x
                                            val newValueY = offset.y + delta.y / scale.value
                                            offset = Offset(
                                                newValueX.coerceIn(0f, tableWidthPx - cellWidthPx),
                                                newValueY.coerceIn(0f, cellHeightPx * 24 - duration)
                                            )

                                            val effectiveColumn =
                                                (offset.x / cellWidthPx).roundToInt()
                                            setSelectedDay(startingDay + effectiveColumn)
                                        }

                                        else -> null.debugAssertNotNull
                                    }
                                    it.consume()
                                }
                                intention = null
                                coroutineScope.launch {
                                    widthReduction.animateTo(defaultWidthReduction)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) addEventRectangle@{
                    val alpha by animateFloatAsState(
                        if (offset == Offset.Unspecified) 0f else 1f,
                        animationSpec = spring(
                            Spring.DampingRatioNoBouncy, Spring.StiffnessLow
                        ), label = "alpha"
                    )
                    if (offset == Offset.Unspecified) return@addEventRectangle
                    val circleBorder = MaterialTheme.colorScheme.surface.copy(alpha = alpha)
                    val background = MaterialTheme.colorScheme.surface.copy(alpha = AppBlendAlpha)
                    val primaryWithAlpha = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    Canvas(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxSize(),
                    ) {
                        val rectTopLeft = Offset(
                            x = if (isRtl) widthReduction.value else 0f,
                            y = if (animatedOffset.y < radius) lineSize else 0f,
                        )
                        val rectSize = Size(
                            width = this.size.width - widthReduction.value,
                            height = this.size.height - heightSizeReductionPx
                        )
                        drawRoundRect(
                            background,
                            size = rectSize,
                            topLeft = rectTopLeft,
                            cornerRadius = CornerRadius(corner),
                        )
                        drawRoundRect(
                            primaryWithAlpha,
                            topLeft = rectTopLeft,
                            size = rectSize,
                            style = Stroke(lineSize),
                            cornerRadius = CornerRadius(corner),
                        )
                        val circleOffset = this.size.width * .05f
                        val offset1 = Offset(
                            x = this.center.x - (this.size.width / 2 - radius - circleOffset) * directionSign,
                            y = if (animatedOffset.y < radius) radius - animatedOffset.y else 0f
                        )
                        drawCircle(circleBorder, radius + lineSize * 2, offset1)
                        drawCircle(primaryWithAlpha, radius, offset1)
                        val offset2 = Offset(
                            x = this.center.x + (this.size.width / 2 - widthReduction.value - radius - circleOffset) * directionSign,
                            y = this.size.height - lineSize * 2
                        )
                        drawCircle(circleBorder, radius + lineSize * 2, offset2)
                        drawCircle(primaryWithAlpha, radius, offset2)
                    }
                    val from = clockCache[y * 15]
                    val to = clockCache[(y + dy) * 15]
                    Text(
                        from + when {
                            dy >= 3 -> "\n"
                            days == 7 -> " "
                            else -> " $EN_DASH "
                        } + to,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            ScrollShadow(scrollState, top = true)
            ScrollShadow(scrollState, top = false)
        }
    }
}

private enum class DragIntention { ExtendUp, ExtendDown, Move }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.WeekPager(
    pagerSize: DpSize,
    addEvent: (AddEventData) -> Unit,
    monthColors: MonthColors,
    selectedDay: Jdn,
    selectedDayDate: AbstractDate,
    setSelectedDay: (Jdn) -> Unit,
    setEverClicked: () -> Unit,
    animatedContentScope: AnimatedContentScope,
    language: Language,
    coroutineScope: CoroutineScope,
    weekPagerState: PagerState,
    page: Int,
    today: Jdn,
    refreshToken: Int,
) {
    Box(Modifier.height(pagerSize.height)) {
        val offset = page - weeksLimit / 2
        val sampleDay = today + 7 * offset
        val startOfYearJdn = Jdn(mainCalendar, selectedDayDate.year, 1, 1)
        val week = sampleDay.getWeekOfYear(startOfYearJdn)

        val isCurrentPage = weekPagerState.currentPage == page
        LaunchedEffect(isCurrentPage) {
            if (isCurrentPage && selectedDay.getWeekOfYear(startOfYearJdn) != week) {
                setSelectedDay(sampleDay + (selectedDay.weekDay - today.weekDay))
            }
        }

        val height = pagerSize.height
        val arrowHeight = height / 2 + (if (language.isArabicScript) 4 else 0).dp
        PagerArrow(arrowHeight, coroutineScope, weekPagerState, page, true, week)
        Box(modifier = Modifier.padding(horizontal = pagerArrowSizeAndPadding.dp)) {
            val monthOffset = mainCalendar.getMonthsDistance(today, selectedDay)
            Month(
                offset = monthOffset,
                width = pagerSize.width - (pagerArrowSizeAndPadding * 2).dp,
                height = height,
                addEvent = addEvent,
                monthColors = monthColors,
                animatedContentScope = animatedContentScope,
                onlyWeek = week,
                today = today,
                isHighlighted = true,
                selectedDay = selectedDay,
                refreshToken = refreshToken,
                setSelectedDay = { setEverClicked(); setSelectedDay(it) },
            )
        }
        PagerArrow(arrowHeight, coroutineScope, weekPagerState, page, false, week)
    }
}

private const val weeksLimit = 25000 // this should be an even number
private const val daysLimit = 175000 // this should be an even number
