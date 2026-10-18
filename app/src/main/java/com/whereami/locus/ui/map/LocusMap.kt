package com.whereami.locus.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.whereami.locus.domain.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng as MapLatLng

private const val STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_PATH = "locus-path"
private const val SOURCE_POSITION = "locus-position"
private const val LAYER_PATH = "locus-path-layer"
private const val LAYER_POSITION_HALO = "locus-position-halo"
private const val LAYER_POSITION = "locus-position-layer"

@Composable
fun LocusMap(
    path: List<LatLng>,
    currentPosition: LatLng?,
    animatePath: Boolean = true,
    fitToPath: Boolean = false,
    recenterToken: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember {
        MapLibre.getInstance(context.applicationContext)
        true
    }

    val pathProgress = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(path, animatePath) {
        if (animatePath && path.size >= 2) {
            pathProgress.snapTo(0f)
            pathProgress.animateTo(1f, animationSpec = tween(durationMillis = 1400))
        } else {
            pathProgress.snapTo(1f)
        }
    }

    val visiblePath = remember(path, pathProgress.value) {
        if (path.isEmpty()) emptyList()
        else {
            val count = ((path.size - 1) * pathProgress.value).toInt().coerceAtLeast(0) + 1
            path.take(count.coerceAtMost(path.size))
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    LaunchedEffect(visiblePath, currentPosition, styleReady, pulseScale, pulseAlpha) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        mapView.getMapAsync { map ->
            val style = map.style ?: return@getMapAsync
            updatePathSource(style, visiblePath)
            updatePositionSource(style, currentPosition, pulseScale, pulseAlpha)
        }
    }

    LaunchedEffect(recenterToken, currentPosition, styleReady) {
        if (recenterToken == 0) return@LaunchedEffect
        val mapView = mapViewRef ?: return@LaunchedEffect
        val target = currentPosition ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    MapLatLng(target.latitude, target.longitude),
                    15.0
                )
            )
        }
    }

    LaunchedEffect(fitToPath, path, styleReady) {
        if (!fitToPath || path.size < 2 || !styleReady) return@LaunchedEffect
        val mapView = mapViewRef ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            val bounds = LatLngBounds.Builder().apply {
                path.forEach { include(MapLatLng(it.latitude, it.longitude)) }
            }.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mapView ->
                    mapView.onCreate(null)
                    mapViewRef = mapView
                    mapView.getMapAsync { map ->
                        map.uiSettings.isLogoEnabled = true
                        map.uiSettings.isAttributionEnabled = true
                        map.uiSettings.isCompassEnabled = false
                        map.setStyle(Style.Builder().fromUri(STYLE_URI)) { style ->
                            ensureLayers(style)
                            styleReady = true
                            if (path.isNotEmpty()) {
                                val target = currentPosition ?: path.last()
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        MapLatLng(target.latitude, target.longitude),
                                        14.0
                                    )
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                mapViewRef = mapView
            }
        )
    }

    MapLifecycle(mapViewRef)
}

@Composable
private fun MapLifecycle(mapView: MapView?) {
    DisposableEffect(mapView) {
        mapView?.onStart()
        mapView?.onResume()
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }
}

private fun ensureLayers(style: Style) {
    if (style.getSource(SOURCE_PATH) == null) {
        style.addSource(GeoJsonSource(SOURCE_PATH))
        style.addLayer(
            LineLayer(LAYER_PATH, SOURCE_PATH).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#1A73E8")),
                PropertyFactory.lineWidth(4.5f),
                PropertyFactory.lineOpacity(0.95f),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round")
            )
        )
    }
    if (style.getSource(SOURCE_POSITION) == null) {
        style.addSource(GeoJsonSource(SOURCE_POSITION))
        style.addLayer(
            CircleLayer(LAYER_POSITION_HALO, SOURCE_POSITION).withProperties(
                PropertyFactory.circleRadius(18f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#1A73E8")),
                PropertyFactory.circleOpacity(0.25f)
            )
        )
        style.addLayer(
            CircleLayer(LAYER_POSITION, SOURCE_POSITION).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#1A73E8")),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE)
            )
        )
    }
}

private fun updatePathSource(style: Style, path: List<LatLng>) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_PATH) ?: return
    if (path.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        return
    }
    val line = LineString.fromLngLats(
        path.map { Point.fromLngLat(it.longitude, it.latitude) }
    )
    source.setGeoJson(Feature.fromGeometry(line))
}

private fun updatePositionSource(
    style: Style,
    position: LatLng?,
    pulseScale: Float,
    pulseAlpha: Float
) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_POSITION) ?: return
    val halo = style.getLayerAs<CircleLayer>(LAYER_POSITION_HALO)
    halo?.setProperties(
        PropertyFactory.circleRadius(12f * pulseScale),
        PropertyFactory.circleOpacity(pulseAlpha)
    )
    if (position == null) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        return
    }
    source.setGeoJson(
        Feature.fromGeometry(Point.fromLngLat(position.longitude, position.latitude))
    )
}
