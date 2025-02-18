package de.hbch.traewelling.ui.searchConnection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hbch.traewelling.R
import de.hbch.traewelling.api.models.station.Station
import de.hbch.traewelling.api.models.trip.HafasLine
import de.hbch.traewelling.api.models.trip.HafasTrip
import de.hbch.traewelling.api.models.trip.HafasTripPage
import de.hbch.traewelling.api.models.trip.ProductType
import de.hbch.traewelling.shared.CheckInViewModel
import de.hbch.traewelling.shared.LoggedInUserViewModel
import de.hbch.traewelling.shared.SettingsViewModel
import de.hbch.traewelling.theme.LocalFont
import de.hbch.traewelling.theme.MainTheme
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.ui.composables.DataLoading
import de.hbch.traewelling.ui.composables.FilterChipGroup
import de.hbch.traewelling.ui.composables.LineIcon
import de.hbch.traewelling.ui.composables.OutlinedButtonWithIconAndText
import de.hbch.traewelling.ui.include.cardSearchStation.CardSearch
import de.hbch.traewelling.util.getDelayColor
import de.hbch.traewelling.util.getLastDestination
import de.hbch.traewelling.util.getLocalTimeString
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

@Composable
fun SearchConnection(
    loggedInUserViewModel: LoggedInUserViewModel,
    checkInViewModel: CheckInViewModel,
    station: Int,
    currentSearchDate: ZonedDateTime,
    onTripSelected: () -> Unit = { },
    onHomelandSelected: (Station) -> Unit = { }
) {
    val viewModel: SearchConnectionViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var timeTableError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hafasTripPage by remember { mutableStateOf<HafasTripPage?>(null) }
    var stationId by rememberSaveable { mutableIntStateOf(station) }
    val stationName by remember { derivedStateOf { hafasTripPage?.meta?.station?.name ?: "" } }
    val trips by remember { derivedStateOf { hafasTripPage?.data ?: listOf() } }
    val times by remember { derivedStateOf { hafasTripPage?.meta?.times } }

    val scrollState = rememberScrollState()
    var searchDate by remember { mutableStateOf(currentSearchDate) }
    var loading by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<FilterType?>(null) }

    LaunchedEffect(stationId, searchDate, selectedFilter) {
        loading = true
        timeTableError = false

        coroutineScope.launch {
            val tripPage = viewModel.searchConnections(stationId, searchDate, selectedFilter)
            loading = false
            hafasTripPage = tripPage.second
            timeTableError = tripPage.first >= 500
            errorMessage = tripPage.third?.message ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardSearch(
            onStationSelected = { station ->
                stationId = station
            },
            homelandStationData = loggedInUserViewModel.home,
            recentStationsData = loggedInUserViewModel.lastVisitedStations,
            queryUsers = false
        )
        if (timeTableError) {
            Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.timetable_api_error),
                textAlign = TextAlign.Center,
                style = LocalFont.current.bodyLarge
            )
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                style = LocalFont.current.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            ElevatedCard {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.departures_at, stationName),
                        style = LocalFont.current.headlineSmall
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loading) {
                        DataLoading()
                    } else {
                        SearchConnection(
                            stationId = stationId,
                            searchTime = searchDate,
                            trips = trips,
                            onPreviousTime = {
                                val time = times?.previous
                                time?.let {
                                    searchDate = it
                                }
                            },
                            onNextTime = {
                                val time = times?.next
                                time?.let {
                                    searchDate = it
                                }
                            },
                            onTripSelection = { trip ->
                                checkInViewModel.reset()
                                checkInViewModel.lineName =
                                    trip.line?.name ?: trip.line?.journeyNumber ?: ""
                                checkInViewModel.lineId = trip.line?.id
                                checkInViewModel.operatorCode = trip.line?.operator?.id
                                checkInViewModel.tripId = trip.tripId
                                checkInViewModel.startStationId = trip.station?.id ?: -1
                                checkInViewModel.departureTime = trip.plannedDeparture
                                checkInViewModel.category =
                                    trip.line?.safeProductType ?: ProductType.UNKNOWN
                                checkInViewModel.origin = trip.station?.name ?: ""

                                onTripSelected()
                            },
                            onTimeSelection = {
                                searchDate = it
                            },
                            onHomelandStationSelection = {
                                coroutineScope.launch {
                                    val s = viewModel.setUserHomelandStation(stationId)
                                    if (s != null) {
                                        loggedInUserViewModel.setHomelandStation(s)
                                        onHomelandSelected(s)
                                    }
                                }
                            },
                            appliedFilter = selectedFilter,
                            onFilter = {
                                selectedFilter = it
                            }
                        )
                    }
                }
            }
        }
        Box { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchConnection(
    modifier: Modifier = Modifier,
    stationId: Int? = null,
    searchTime: ZonedDateTime = ZonedDateTime.now(),
    trips: List<HafasTrip>? = null,
    onPreviousTime: () -> Unit = { },
    onNextTime: () -> Unit = { },
    appliedFilter: FilterType? = null,
    onFilter: (FilterType?) -> Unit = { },
    onTripSelection: (HafasTrip) -> Unit = { },
    onHomelandStationSelection: () -> Unit = { },
    onTimeSelection: (ZonedDateTime) -> Unit = { }
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = context as ViewModelStoreOwner
    )
    val displayDivergentStop by settingsViewModel.displayDivergentStop.observeAsState(true)

    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = searchTime.toInstant().toEpochMilli()
    )
    var timePickerVisible by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = searchTime.hour,
        initialMinute = searchTime.minute
    )

    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerVisible = false
                        timePickerVisible = true
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(
                        text = stringResource(id = R.string.ok)
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (timePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { timePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        timePickerVisible = false

                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            var dateTime = Instant
                                .ofEpochMilli(selectedDate)
                                .atZone(ZoneId.systemDefault())

                            dateTime = dateTime.withHour(timePickerState.hour)
                            dateTime = dateTime.withMinute(timePickerState.minute)

                            onTimeSelection(dateTime)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.ok)
                    )
                }
            }
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val itemModifier = Modifier.padding(horizontal = 8.dp)
        // Time selection and home
        Row(
            modifier = itemModifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ButtonWithIconAndText(
                drawableId = R.drawable.ic_time,
                text = getLocalTimeString(searchTime),
                onClick = {
                    datePickerVisible = true
                }
            )
            IconButton(onClick = onHomelandStationSelection) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = null
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        // Filter chips
        FilterChipGroup(
            modifier = itemModifier
                .fillMaxWidth(),
            chips = FilterType.entries.associateWith {
                 stringResource(id = it.stringId)
            },
            preSelection = appliedFilter,
            selectionRequired = false,
            onSelectionChanged = onFilter
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        // Previous/Next top
        PreviousNextButtons(
            modifier = itemModifier
                .fillMaxWidth(),
            nextSelected = onNextTime,
            previousSelected = onPreviousTime
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        // Connections
        trips?.forEach { trip ->
            ConnectionListItem(
                modifier = itemModifier
                    .fillMaxWidth()
                    .clickable {
                        if (!trip.isCancelled) {
                            onTripSelection(trip)
                        }
                    }
                    .padding(vertical = 8.dp),
                productType = trip.line?.safeProductType ?: ProductType.UNKNOWN,
                departurePlanned = trip.plannedDeparture ?: ZonedDateTime.now(),
                departureReal = trip.departure ?: trip.plannedDeparture,
                isCancelled = trip.isCancelled,
                destination = getLastDestination(trip),
                departureStation =
                    if (!trip.station?.name.isNullOrBlank() && stationId != null && trip.station?.id != stationId && displayDivergentStop)
                        trip.station?.name
                    else
                        null,
                hafasLine = trip.line,
                platformPlanned = trip.plannedPlatform,
                platformReal = trip.platform
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (trips.isNullOrEmpty()) {
            Text(
                text = stringResource(id = R.string.no_departures),
                modifier = itemModifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Previous/Next bottom
        PreviousNextButtons(
            modifier = itemModifier
                .fillMaxWidth(),
            nextSelected = onNextTime,
            previousSelected = onPreviousTime
        )
    }
}

@Composable
fun ConnectionListItem(
    productType: ProductType,
    departurePlanned: ZonedDateTime,
    departureReal: ZonedDateTime?,
    isCancelled: Boolean,
    destination: String,
    departureStation: String?,
    hafasLine: HafasLine?,
    platformPlanned: String?,
    platformReal: String?,
    modifier: Modifier = Modifier
) {
    val journeyNumber = hafasLine?.journeyNumber
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Product image, line and time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = productType.getIcon()),
                    contentDescription = stringResource(id = productType.getString())
                )
                LineIcon(
                    lineName = hafasLine?.name ?: "",
                    operatorCode = hafasLine?.operator?.id,
                    lineId = hafasLine?.id,
                    journeyNumber = journeyNumber
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text =
                    if (isCancelled) stringResource(id = R.string.cancelled)
                    else getLocalTimeString(departureReal ?: departurePlanned),
                    color =
                    if (isCancelled) Color.Red
                    else getDelayColor(planned = departurePlanned, real = departureReal)
                )
                if (isCancelled || (departureReal ?: departurePlanned) > departurePlanned) {
                    Text(
                        text = getLocalTimeString(
                            date = departurePlanned
                        ),
                        textDecoration = TextDecoration.LineThrough,
                        style = LocalFont.current.labelMedium
                    )
                }
            }
        }

        // Direction and departure from different station
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = destination,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (departureStation != null) {
                        Text(
                            text = stringResource(id = R.string.from_station, departureStation),
                            style = LocalFont.current.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Platform
            if (!isCancelled) {
                Platform(planned = platformPlanned, real = platformReal)
            }
        }
    }
}

@Composable
fun Platform(
    planned: String?,
    real: String?
) {
    if (!planned.isNullOrBlank() || !real.isNullOrBlank()) {
        val color = if (real != null && planned != null && real != planned) Color.Red else Color.Blue
        Box(
            modifier = Modifier
                .background(
                    color = color,
                    shape = RoundedCornerShape(percent = 30)
                )
                .padding(vertical = 2.dp, horizontal = 4.dp)
        ) {
            Text(
                text = stringResource(
                    id = R.string.platform,
                    real ?: planned ?: ""
                ),
                color = Color.White,
                style = LocalFont.current.labelSmall
            )
        }
    }
}

@Composable
private fun PreviousNextButtons(
    modifier: Modifier = Modifier,
    previousSelected: () -> Unit = { },
    nextSelected: () -> Unit = { }
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButtonWithIconAndText(
            onClick = previousSelected,
            text = stringResource(id = R.string.previous),
            drawableId = R.drawable.ic_previous
        )
        OutlinedButtonWithIconAndText(
            onClick = nextSelected,
            text = stringResource(id = R.string.next),
            drawableId = R.drawable.ic_next,
            drawableOnStart = false
        )
    }
}

enum class FilterType {
    EXPRESS {
        override val stringId = R.string.product_type_express
        override val filterQuery = "express"
    },
    REGIONAL {
        override val stringId = R.string.product_type_regional
        override val filterQuery = "regional"
    },
    SUBURBAN {
        override val stringId = R.string.product_type_suburban
        override val filterQuery = "suburban"
    },
    SUBWAY {
        override val stringId = R.string.product_type_subway
        override val filterQuery = "subway"
    },
    TRAM {
        override val stringId = R.string.product_type_tram
        override val filterQuery = "tram"
    },
    BUS {
        override val stringId = R.string.product_type_bus
        override val filterQuery = "bus"
    },
    FERRY {
        override val stringId = R.string.product_type_ferry
        override val filterQuery = "ferry"
    };

    abstract val stringId: Int
    abstract val filterQuery: String
}

@Preview
@Composable
fun SearchConnectionPreview() {
    MainTheme {
        SearchConnection()
    }
}

@Preview
@Composable
fun ConnectionListItemPreview() {
    MainTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ConnectionListItem(
                productType = ProductType.BUS,
                departurePlanned = ZonedDateTime.now(),
                departureReal = ZonedDateTime.now(),
                isCancelled = false,
                destination = "Memmingen",
                departureStation = null,
                hafasLine = null,
                platformPlanned = "2",
                platformReal = "3 Süd"
            )
            ConnectionListItem(
                productType = ProductType.TRAM,
                departurePlanned = ZonedDateTime.now(),
                departureReal = ZonedDateTime.now(),
                isCancelled = true,
                destination = "S-Vaihingen über Dachswald, Panoramabahn etc pp",
                departureStation = "Hauptbahnhof, Arnulf-Klett-Platz, einmal über den Fernwanderweg, rechts abbiegen, Treppe runter, dritter Bahnsteig rechts",
                hafasLine = null,
                platformPlanned = "2",
                platformReal = "2"
            )
        }
    }
}
