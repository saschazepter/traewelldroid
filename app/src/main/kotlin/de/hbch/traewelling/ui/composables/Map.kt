package de.hbch.traewelling.ui.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jcloquell.androidsecurestorage.SecureStorage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import de.hbch.traewelling.BuildConfig
import de.hbch.traewelling.R
import de.hbch.traewelling.shared.SharedValues
import de.hbch.traewelling.theme.PolylineColor
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapRuntimeOptions
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.createMapRuntime
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.resource.MapRequestInterceptor
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.sources.rememberRasterTileSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

@Composable
fun OpenRailwayMapView(
    modifier: Modifier = Modifier,
    initialCameraPosition: CameraPosition = CameraPosition(),
    onMapReady: (MapState) -> Unit = { },
    content: @Composable @org.maplibre.compose.util.MaplibreComposable () -> Unit = {}
) {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    val selectedOrmLayer = remember {
        secureStorage.getObject(SharedValues.SS_ORM_LAYER, OpenRailwayMapLayer::class.java)
            ?: OpenRailwayMapLayer.STANDARD
    }

    var ormEnabled by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()
    val baseStyleUri = if (isDark) {
        "https://tiles.openfreemap.org/styles/dark"
    } else {
        "https://tiles.openfreemap.org/styles/liberty"
    }

    val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
    val mapRuntime = remember {
        createMapRuntime(
            MapRuntimeOptions(
                requestInterceptor = MapRequestInterceptor(
                    headers = { mapOf("User-Agent" to userAgent) }
                )
            )
        )
    }

    val mapState = rememberMapState(
        runtime = mapRuntime,
        baseStyle = BaseStyle.Uri(baseStyleUri),
        initialCameraPosition = initialCameraPosition
    ) {
        val ormSource = rememberRasterTileSource(
            tiles = listOf("https://tiles.openrailwaymap.org/${selectedOrmLayer.key}/{z}/{x}/{y}.png"),
            options = TileSetOptions(
                attributionHtml = "<a href=\"https://www.openrailwaymap.org/\">OpenRailwayMap</a> (<a href=\"https://creativecommons.org/licenses/by-sa/2.0/\">CC-BY-SA 2.0</a>)"
            ),
            tileSize = 256
        )
        RasterLayer(
            id = "orm-layer",
            source = ormSource,
            visible = ormEnabled
        )

        content()
    }

    MaplibreMap(
        modifier = modifier,
        state = mapState,
        overlay = {
            Box(Modifier.fillMaxSize()) {
                MaplibreLogo(Modifier.align(Alignment.BottomStart).padding(2.dp))
                ExpandingAttributionButton(Modifier.align(Alignment.BottomEnd).padding(2.dp))

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DisappearingCompassButton()
                    FilledIconToggleButton(
                        checked = ormEnabled,
                        onCheckedChange = { ormEnabled = it }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_train),
                            contentDescription = "Toggle OpenRailwayMap"
                        )
                    }
                }
            }
        }
    )

    LaunchedEffect(mapState) {
        onMapReady(mapState)
    }
}

enum class OpenRailwayMapLayer {
    STANDARD {
        override val key = "standard"
        override val tracksValue = "usage"
        override val title = R.string.standard_layer
        override val description = R.string.standard_description
    },
    SIGNALS {
        override val key = "signals"
        override val tracksValue = "train_protection"
        override val title = R.string.signal_layer
        override val description = R.string.signal_description
    },
    MAXSPEED {
        override val key = "maxspeed"
        override val tracksValue = "speed"
        override val title = R.string.maxspeed_layer
        override val description = R.string.maxspeed_description
    };

    abstract val key: String
    abstract val tracksValue: String
    abstract val title: Int
    abstract val description: Int
}

fun List<Position>.toBoundingBox(marginDegrees: Double = 0.01): BoundingBox? {
    val valid = filter { (it.latitude != 0.0 || it.longitude != 0.0) && it.latitude.isFinite() && it.longitude.isFinite() }
    if (valid.isEmpty()) return null
    var minLat = valid.minOf { it.latitude }
    var maxLat = valid.maxOf { it.latitude }
    var minLng = valid.minOf { it.longitude }
    var maxLng = valid.maxOf { it.longitude }

    if (maxLat - minLat < marginDegrees) {
        val pad = (marginDegrees - (maxLat - minLat)) / 2.0
        minLat -= pad
        maxLat += pad
    }
    if (maxLng - minLng < marginDegrees) {
        val pad = (marginDegrees - (maxLng - minLng)) / 2.0
        minLng -= pad
        maxLng += pad
    }

    return BoundingBox(west = minLng, south = minLat, east = maxLng, north = maxLat)
}

fun List<Position>.toInitialCameraPosition(zoomOffset: Double = 1.2): CameraPosition? {
    val valid = filter { (it.latitude != 0.0 || it.longitude != 0.0) && it.latitude.isFinite() && it.longitude.isFinite() }
    if (valid.isEmpty()) return null

    val minLat = valid.minOf { it.latitude }
    val maxLat = valid.maxOf { it.latitude }
    val minLng = valid.minOf { it.longitude }
    val maxLng = valid.maxOf { it.longitude }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLng = (minLng + maxLng) / 2.0

    val deltaLat = (maxLat - minLat).coerceAtLeast(0.01)
    val deltaLng = (maxLng - minLng).coerceAtLeast(0.01)
    val maxDelta = maxOf(deltaLat, deltaLng)

    val zoom = (kotlin.math.ln(360.0 / maxDelta) / kotlin.math.ln(2.0) - zoomOffset).coerceIn(1.0, 18.0)

    return CameraPosition(
        target = Position(longitude = centerLng, latitude = centerLat),
        zoom = zoom
    )
}

fun extractPositionsFromGeoJson(element: com.google.gson.JsonElement?): List<Position> {
    if (element == null || element.isJsonNull) return emptyList()
    val positions = mutableListOf<Position>()

    fun parseCoords(array: com.google.gson.JsonArray) {
        if (array.size() >= 2 && array[0].isJsonPrimitive && array[1].isJsonPrimitive && array[0].asJsonPrimitive.isNumber && array[1].asJsonPrimitive.isNumber) {
            val lng = array[0].asDouble
            val lat = array[1].asDouble
            if ((lat != 0.0 || lng != 0.0) && lat.isFinite() && lng.isFinite()) {
                positions.add(Position(lng, lat))
            }
        } else {
            for (item in array) {
                if (item.isJsonArray) {
                    parseCoords(item.asJsonArray)
                }
            }
        }
    }

    if (element.isJsonObject) {
        val obj = element.asJsonObject
        if (obj.has("features") && obj.get("features").isJsonArray) {
            obj.getAsJsonArray("features").forEach { f ->
                if (f.isJsonObject) {
                    val geom = f.asJsonObject.getAsJsonObject("geometry")
                    if (geom != null && geom.has("coordinates") && geom.get("coordinates").isJsonArray) {
                        parseCoords(geom.getAsJsonArray("coordinates"))
                    }
                }
            }
        } else if (obj.has("geometry") && obj.get("geometry").isJsonObject) {
            val geom = obj.getAsJsonObject("geometry")
            if (geom != null && geom.has("coordinates") && geom.get("coordinates").isJsonArray) {
                parseCoords(geom.getAsJsonArray("coordinates"))
            }
        } else if (obj.has("coordinates") && obj.get("coordinates").isJsonArray) {
            parseCoords(obj.getAsJsonArray("coordinates"))
        }
    } else if (element.isJsonArray) {
        parseCoords(element.asJsonArray)
    }

    return positions
}

@Composable
@org.maplibre.compose.util.MaplibreComposable
fun RoutePolylineLayer(
    geoJson: Any?,
    defaultColorHex: String? = null,
    fallbackColor: Color = PolylineColor,
    layerId: String = "traewelldroid-route",
    lineWidth: Dp = 4.dp
) {
    if (geoJson == null) return

    val fallbackHex = remember(fallbackColor, defaultColorHex) {
        val baseHex = defaultColorHex ?: String.format("#%06X", 0xFFFFFF and fallbackColor.toArgb())
        if (baseHex.startsWith("#")) baseHex else "#$baseHex"
    }

    val geoJsonString = remember(geoJson, fallbackHex) {
        val gson = com.google.gson.Gson()
        val jsonElement = if (geoJson is com.google.gson.JsonElement) geoJson else gson.toJsonTree(geoJson)

        fun processFeature(feature: com.google.gson.JsonObject) {
            var props = feature.getAsJsonObject("properties")
            if (props == null) {
                props = com.google.gson.JsonObject()
                feature.add("properties", props)
            }
            if (!props.has("lineColor") || props.get("lineColor").isJsonNull) {
                props.addProperty("lineColor", fallbackHex)
            } else {
                val existing = props.get("lineColor").asString
                if (!existing.startsWith("#")) {
                    props.addProperty("lineColor", "#$existing")
                }
            }
        }

        if (jsonElement is com.google.gson.JsonObject) {
            val type = jsonElement.get("type")?.asString
            if (type == "FeatureCollection" && jsonElement.has("features")) {
                jsonElement.getAsJsonArray("features")?.forEach { f ->
                    if (f is com.google.gson.JsonObject) {
                        processFeature(f)
                    }
                }
            } else if (type == "Feature") {
                processFeature(jsonElement)
            }
        }
        jsonElement.toString()
    }

    val source = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(geoJsonString)
    )

    LineLayer(
        id = layerId,
        source = source,
        color = feature["lineColor"]
            .asString(const(fallbackHex))
            .convertToColor(),
        width = const(lineWidth)
    )
}
