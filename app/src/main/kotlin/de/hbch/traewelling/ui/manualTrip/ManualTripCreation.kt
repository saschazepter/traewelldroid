package de.hbch.traewelling.ui.manualTrip

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hbch.traewelling.R
import de.hbch.traewelling.api.models.polyline.Feature
import de.hbch.traewelling.api.models.station.Station
import de.hbch.traewelling.api.models.trip.CreateManualTripRequest
import de.hbch.traewelling.api.models.trip.ManualTripStopover
import de.hbch.traewelling.api.models.trip.Operator
import de.hbch.traewelling.api.models.trip.PreviewManualTripPolylineRequest
import de.hbch.traewelling.api.models.trip.ProductType
import de.hbch.traewelling.api.models.trip.Trip
import de.hbch.traewelling.theme.PolylineColor
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.ui.composables.DateTimeSelection
import de.hbch.traewelling.ui.composables.OpenRailwayMapView
import de.hbch.traewelling.ui.composables.OutlinedButtonWithIconAndText
import de.hbch.traewelling.ui.composables.getBoundingBoxFromPolyLines
import de.hbch.traewelling.ui.composables.getPolyLineFromFeature
import de.hbch.traewelling.ui.search.SearchViewModel
import de.hbch.traewelling.util.useDebounce
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.time.ZonedDateTime
import java.util.UUID

private data class InternalStopover(
    val id: UUID = UUID.randomUUID(),
    val station: Station? = null,
    val arrival: ZonedDateTime? = null,
    val departure: ZonedDateTime? = null,
    val stationError: Boolean = false,
    val arrivalError: Boolean = false,
    val departureError: Boolean = false
)

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTripCreation(
    modifier: Modifier = Modifier,
    onTripCreated: (Trip) -> Unit = { },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val viewModel: ManualTripCreationViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var columnModifier = modifier
    if (selectedTab == 0)
        columnModifier = columnModifier.verticalScroll(rememberScrollState())

    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = stringResource(id = R.string.trip_data)
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_route),
                        contentDescription = null
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = stringResource(id = R.string.preview)
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_map),
                        contentDescription = null
                    )
                }
            )
        }

        // Form data
        var selectedProductType by rememberSaveable { mutableStateOf<ProductType?>(null) }
        var productTypeError by remember { mutableStateOf(false) }
        val lineNameTextFieldState = rememberTextFieldState()
        var lineNameError by remember { mutableStateOf(false) }
        val journeyNumberTextFieldState = rememberTextFieldState()
        var selectedOperator by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

        val operatorTextFieldState = rememberTextFieldState()
        var debouncedOperatorQuery by remember { mutableStateOf("") }
        operatorTextFieldState.useDebounce {
            debouncedOperatorQuery = it.text.toString()
        }
        val operatorSuggestions = remember { mutableStateListOf<Operator>() }
        var operatorSuggestionsVisible by remember { mutableStateOf(false) }

        var wasLineNameChanged by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(lineNameTextFieldState.text) {
            if (lineNameTextFieldState.text.isNotEmpty()) {
                wasLineNameChanged = true
            }
            lineNameError = wasLineNameChanged && lineNameTextFieldState.text.isBlank()
        }

        var departureStation by remember { mutableStateOf<Station?>(null) }
        var departureTime by remember { mutableStateOf<ZonedDateTime?>(null) }
        var arrivalStation by remember { mutableStateOf<Station?>(null) }
        var arrivalTime by remember { mutableStateOf<ZonedDateTime?>(null) }

        var departureStationError by remember { mutableStateOf(false) }
        var departureTimeError by remember { mutableStateOf(false) }
        var arrivalStationError by remember { mutableStateOf(false) }
        var arrivalTimeError by remember { mutableStateOf(false) }

        val stopovers = remember { mutableStateListOf<InternalStopover>() }
        var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val itemHeights = remember { mutableStateMapOf<UUID, Int>() }

        LaunchedEffect(debouncedOperatorQuery) {
            val query = debouncedOperatorQuery
            if (query.isBlank() || query == selectedOperator?.second)
                return@LaunchedEffect

            val operators = viewModel.getOperators(query)
            if (operators.isNullOrEmpty())
                return@LaunchedEffect

            operatorSuggestions.clear()
            operatorSuggestions.addAll(operators)
            operatorSuggestionsVisible = true
        }

        if (selectedTab == 0) {
            val formModifier = Modifier.fillMaxWidth()

            // Card for basic trip data
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.trip_data)
                    )
                    // Product Type
                    var productTypeSelectionVisible by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = productTypeSelectionVisible,
                        onExpandedChange = { productTypeSelectionVisible = it }
                    ) {
                        val productTypeInteractionSource = remember { MutableInteractionSource() }
                        val productTypeFieldPressed by productTypeInteractionSource.collectIsPressedAsState()
                        if (productTypeFieldPressed) {
                            productTypeSelectionVisible = true
                            productTypeError = false
                        }

                        val productType = selectedProductType
                        OutlinedTextField(
                            value = if (productType == null) "" else stringResource(productType.text),
                            onValueChange = { },
                            modifier = formModifier.clickable(
                                productTypeInteractionSource,
                                null
                            ) { },
                            leadingIcon = {
                                if (productType != null) {
                                    Image(
                                        painter = painterResource(id = productType.icon),
                                        contentDescription = null
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.product_type)
                                )
                            },
                            singleLine = true,
                            interactionSource = productTypeInteractionSource,
                            readOnly = true,
                            isError = productTypeError
                        )
                        ExposedDropdownMenu(
                            expanded = productTypeSelectionVisible,
                            onDismissRequest = {
                                productTypeSelectionVisible = false
                            }
                        ) {
                            ProductType.entries.filter { it.selectable }.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(id = it.text)
                                        )
                                    },
                                    leadingIcon = {
                                        Image(
                                            painter = painterResource(id = it.icon),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        selectedProductType = it
                                        productTypeSelectionVisible = false
                                    }
                                )
                            }
                        }
                    }

                    // Line name and journey number
                    Row(
                        modifier = formModifier,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            state = lineNameTextFieldState,
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = stringResource(id = R.string.line_train_name)
                                )
                            },
                            isError = lineNameError
                        )
                        OutlinedTextField(
                            state = journeyNumberTextFieldState,
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = stringResource(id = R.string.journey_number)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    // Operator
                    ExposedDropdownMenuBox(
                        expanded = operatorSuggestionsVisible,
                        onExpandedChange = { operatorSuggestionsVisible = it }
                    ) {
                        OutlinedTextField(
                            state = operatorTextFieldState,
                            modifier = formModifier,
                            label = {
                                Text(
                                    text = stringResource(id = R.string.operators)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            readOnly = selectedOperator != null,
                            trailingIcon = {
                                if (selectedOperator != null) {
                                    IconButton(
                                        onClick = {
                                            selectedOperator = null
                                            operatorTextFieldState.clearText()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.delete)
                                        )
                                    }
                                }
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = operatorSuggestionsVisible,
                            onDismissRequest = { operatorSuggestionsVisible = false }
                        ) {
                            operatorSuggestions.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = it.name
                                        )
                                    },
                                    onClick = {
                                        selectedOperator = Pair(it.uuid, it.name)
                                        operatorTextFieldState.setTextAndPlaceCursorAtEnd(it.name)
                                        operatorSuggestionsVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Departure station
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.departure_station)
                    )
                    StationTimeSelection(
                        label = stringResource(R.string.departure_station),
                        station = departureStation,
                        onStationChange = {
                            departureStation = it
                            departureStationError = false
                        },
                        departure = departureTime,
                        onDepartureChange = {
                            departureTime = it
                            departureTimeError = false
                        },
                        modifier = formModifier,
                        stationError = departureStationError,
                        departureError = departureTimeError
                    )
                }
            }

            // Stopovers
            stopovers.forEachIndexed { index, stopover ->
                key(stopover.id) {
                    val isDragging = draggedItemIndex == index
                    val currentIndexState = rememberUpdatedState(index)
                    val animatedScale by animateFloatAsState(if (isDragging) 1.02f else 1f, label = "scale")

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                itemHeights[stopover.id] = it.size.height
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffset
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = 0.9f
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(
                                modifier = formModifier,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_drag),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .pointerInput(stopover.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedItemIndex = currentIndexState.value
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y

                                                    val currentPos =
                                                        draggedItemIndex ?: return@detectDragGesturesAfterLongPress

                                                    val spacing = 8.dp.toPx()
                                                    if (dragOffset > 0 && currentPos < stopovers.size - 1) {
                                                        val nextStopover = stopovers[currentPos + 1]
                                                        val nextHeight = itemHeights[nextStopover.id] ?: 0
                                                        if (dragOffset > (nextHeight + spacing) / 2f) {
                                                            val item = stopovers.removeAt(currentPos)
                                                            stopovers.add(currentPos + 1, item)
                                                            draggedItemIndex = currentPos + 1
                                                            dragOffset -= (nextHeight + spacing)
                                                        }
                                                    } else if (dragOffset < 0 && currentPos > 0) {
                                                        val prevStopover = stopovers[currentPos - 1]
                                                        val prevHeight = itemHeights[prevStopover.id] ?: 0
                                                        if (dragOffset < -(prevHeight + spacing) / 2f) {
                                                            val item = stopovers.removeAt(currentPos)
                                                            stopovers.add(currentPos - 1, item)
                                                            draggedItemIndex = currentPos - 1
                                                            dragOffset += (prevHeight + spacing)
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                }
                                            )
                                        }
                                )
                                Text(
                                    text = "${stringResource(R.string.stopover)} #${index + 1}",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    itemHeights.remove(stopover.id)
                                    stopovers.removeAt(currentIndexState.value)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null
                                    )
                                }
                            }
                            StationTimeSelection(
                                label = stringResource(R.string.stopover),
                                station = stopover.station,
                                onStationChange = {
                                    stopovers[currentIndexState.value] = stopover.copy(station = it, stationError = false)
                                },
                                arrival = stopover.arrival,
                                onArrivalChange = {
                                    stopovers[currentIndexState.value] = stopover.copy(arrival = it, arrivalError = false)
                                },
                                departure = stopover.departure,
                                onDepartureChange = {
                                    stopovers[currentIndexState.value] = stopover.copy(departure = it, departureError = false)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                stationError = stopover.stationError,
                                arrivalError = stopover.arrivalError,
                                departureError = stopover.departureError
                            )
                        }
                    }
                }
            }
            OutlinedButtonWithIconAndText(
                modifier = formModifier,
                text = stringResource(R.string.add_stopover),
                drawableId = R.drawable.ic_add,
                onClick = { stopovers.add(InternalStopover()) }
            )

            // Arrival station
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.arrival_station)
                    )
                    StationTimeSelection(
                        label = stringResource(R.string.arrival_station),
                        station = arrivalStation,
                        onStationChange = {
                            arrivalStation = it
                            arrivalStationError = false
                        },
                        arrival = arrivalTime,
                        onArrivalChange = {
                            arrivalTime = it
                            arrivalTimeError = false
                        },
                        modifier = formModifier,
                        stationError = arrivalStationError,
                        arrivalError = arrivalTimeError
                    )
                }
            }

            // Save button
            ButtonWithIconAndText(
                modifier = formModifier,
                text = stringResource(R.string.save),
                drawableId = R.drawable.ic_check,
                onClick = {
                    coroutineScope.launch {
                        val productType = selectedProductType
                        val lineName = lineNameTextFieldState.text.toString()
                        val journeyNumber = journeyNumberTextFieldState.text.toString().toLongOrNull()
                        val operator = selectedOperator?.first
                        val depTime = departureTime
                        val depStationId = departureStation?.id
                        val arrTime = arrivalTime
                        val arrStationId = arrivalStation?.id

                        if (productType == null) {
                            productTypeError = true
                            return@launch
                        }
                        if (lineName.isBlank()) {
                            wasLineNameChanged = true
                            lineNameError = true
                            return@launch
                        }
                        if (depStationId == null) {
                            departureStationError = true
                        }
                        if (depTime == null) {
                            departureTimeError = true
                        }
                        if (arrTime == null) {
                            arrivalTimeError = true
                        }
                        if (arrStationId == null) {
                            arrivalStationError = true
                        }

                        if (depStationId == null || depTime == null || arrTime == null || arrStationId == null) {
                            return@launch
                        }

                        // Temporal validation
                        var anyTemporalError = false
                        val validatedStopovers = stopovers.map {
                            it.copy(stationError = false, arrivalError = false, departureError = false)
                        }.toMutableList()

                        var currentLastTime = depTime

                        for (i in validatedStopovers.indices) {
                            val stopover = validatedStopovers[i]
                            if (stopover.station == null) continue

                            var aError = false
                            var dError = false

                            if (stopover.arrival != null) {
                                if (stopover.arrival.isBefore(currentLastTime)) {
                                    aError = true
                                    anyTemporalError = true
                                }
                                currentLastTime = stopover.arrival
                            }

                            if (stopover.departure != null) {
                                if (stopover.departure.isBefore(currentLastTime)) {
                                    dError = true
                                    anyTemporalError = true
                                }
                                currentLastTime = stopover.departure
                            }

                            validatedStopovers[i] = validatedStopovers[i].copy(
                                arrivalError = aError,
                                departureError = dError
                            )
                        }

                        if (arrTime.isBefore(currentLastTime)) {
                            arrivalTimeError = true
                            anyTemporalError = true
                        }

                        if (anyTemporalError) {
                            stopovers.clear()
                            stopovers.addAll(validatedStopovers)
                            return@launch
                        }

                        val trip = viewModel.createManualTrip(
                            CreateManualTripRequest(
                                productType,
                                lineName,
                                journeyNumber,
                                operator,
                                depStationId,
                                depTime,
                                arrStationId,
                                arrTime,
                                stopovers.filter { it.station != null }.map {
                                    ManualTripStopover(
                                        it.station!!.id,
                                        it.arrival,
                                        it.departure
                                    )
                                }
                            )
                        )
                        if (trip == null) {
                            snackbarHostState.showSnackbar(context.getString(R.string.error_manual_trip_validation))
                            return@launch
                        }
                        onTripCreated(trip)
                    }
                }
            )
        } else {
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var errorText by remember { mutableStateOf("") }
            var feature by remember { mutableStateOf<Feature?>(null) }

            LaunchedEffect(Unit) {
                val departureId = departureStation?.id
                val arrivalId = arrivalStation?.id
                val productType = selectedProductType
                val stopoverIds = stopovers.filter { it.station != null }.map { it.station!!.id }.toTypedArray()

                if (departureId == null || arrivalId == null || productType == null) {
                    errorText = context.getString(R.string.error_manual_trip_validation)
                    return@LaunchedEffect
                }

                val stationIds = mutableListOf(
                    departureId,
                    *stopoverIds,
                    arrivalId
                )
                val feat = viewModel.requestPolylinePreview(
                    PreviewManualTripPolylineRequest(productType, stationIds)
                )
                if (feat == null) {
                    errorText = context.getString(R.string.error_occurred)
                    return@LaunchedEffect
                }
                feature = feat
                errorText = ""
            }

            LaunchedEffect(feature, mapView) {
                val feat = feature ?: return@LaunchedEffect
                val map = mapView ?: return@LaunchedEffect

                val polyline =
                    getPolyLineFromFeature(feat, PolylineColor.toArgb()) ?: return@LaunchedEffect
                val bounds = getBoundingBoxFromPolyLines(listOf(polyline))

                map.overlays.filterIsInstance<Polyline>().forEach { map.overlays.remove(it) }
                map.overlays.add(polyline)
                map.zoomToBoundingBox(bounds.increaseByScale(1.2f), false)
                map.invalidate()
            }
            if (feature != null) {
                ElevatedCard(
                    modifier = modifier
                        .fillMaxWidth(),
                ) {
                    OpenRailwayMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        onLoad = {
                            mapView = it
                        }
                    )
                }
            } else if (errorText.isNotBlank()) {
                Text(text = errorText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationSelection(
    label: String,
    station: Station?,
    onStationChange: (Station?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val viewModel: SearchViewModel = viewModel()
    val stationSearchQuery = rememberTextFieldState(station?.name ?: "")
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var stationSuggestionsVisible by remember { mutableStateOf(false) }
    val stationSuggestions = remember { mutableStateListOf<Station>() }

    stationSearchQuery.useDebounce {
        debouncedSearchQuery = it.text.toString()
    }

    LaunchedEffect(debouncedSearchQuery) {
        val q = debouncedSearchQuery
        if (q.isBlank() || (station != null && q == station.name)) {
            stationSuggestionsVisible = false
            return@LaunchedEffect
        }

        // Request station autocomplete
        val stations = viewModel.searchStations(q)
        stationSuggestions.clear()
        if (!stations.isNullOrEmpty()) {
            stationSuggestions.addAll(stations)
            stationSuggestionsVisible = true
        } else {
            stationSuggestionsVisible = false
        }
    }

    LaunchedEffect(station) {
        if (station != null && stationSearchQuery.text != station.name) {
            stationSearchQuery.setTextAndPlaceCursorAtEnd(station.name)
        }
    }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = stationSuggestionsVisible,
        onExpandedChange = { stationSuggestionsVisible = it }
    ) {
        OutlinedTextField(
            state = stationSearchQuery,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            label = {
                Text(text = label)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = isError,
            readOnly = station != null,
            trailingIcon = {
                if (station != null) {
                    IconButton(onClick = { onStationChange(null) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null
                        )
                    }
                }
            }
        )
        ExposedDropdownMenu(
            expanded = stationSuggestionsVisible,
            onDismissRequest = { stationSuggestionsVisible = false }
        ) {
            stationSuggestions.forEach {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = it.name
                        )
                    },
                    onClick = {
                        onStationChange(it)
                        stationSearchQuery.setTextAndPlaceCursorAtEnd(it.name)
                        stationSuggestionsVisible = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StationTimeSelection(
    label: String,
    station: Station?,
    onStationChange: (Station?) -> Unit,
    modifier: Modifier = Modifier,
    arrival: ZonedDateTime? = null,
    onArrivalChange: ((ZonedDateTime) -> Unit)? = null,
    departure: ZonedDateTime? = null,
    onDepartureChange: ((ZonedDateTime) -> Unit)? = null,
    stationError: Boolean = false,
    arrivalError: Boolean = false,
    departureError: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StationSelection(
            label = label,
            station = station,
            onStationChange = onStationChange,
            modifier = Modifier.fillMaxWidth(),
            isError = stationError
        )

        if (onArrivalChange != null || onDepartureChange != null) {
            if (onArrivalChange != null) {
                DateTimeSelection(
                    initDate = arrival,
                    plannedDate = null,
                    label = R.string.arrival_time,
                    modifier = Modifier.fillMaxWidth(),
                    dateSelected = {
                        if (it != null) {
                            onArrivalChange(it)
                        }
                    },
                    showNow = false,
                    isError = arrivalError
                )
            }
            if (onDepartureChange != null) {
                DateTimeSelection(
                    initDate = departure,
                    plannedDate = null,
                    label = R.string.departure_time,
                    modifier = Modifier.fillMaxWidth(),
                    dateSelected = {
                        if (it != null) {
                            onDepartureChange(it)
                        }
                    },
                    showNow = false,
                    isError = departureError
                )
            }
        }
    }
}
