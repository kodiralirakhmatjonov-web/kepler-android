package com.iumrah.beta.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Shared native map host for Android parity with the iOS MapKit surfaces.
 *
 * MapLibre is initialized without a vendor token and uses OpenFreeMap for the
 * standard vector style. Satellite imagery uses the open VersaTiles raster
 * tileset, with source attribution preserved in the style.
 */
enum class IumrahMapStyle { STANDARD, SATELLITE }

internal const val IUMRAH_STANDARD_MAP_STYLE = "https://tiles.openfreemap.org/styles/positron"

internal val IUMRAH_SATELLITE_MAP_STYLE_JSON: String = """
{
  "version": 8,
  "name": "iumrah Satellite",
  "sources": {
    "versatiles-satellite": {
      "type": "raster",
      "tiles": ["https://tiles.versatiles.org/tiles/satellite/{z}/{x}/{y}"],
      "tileSize": 512,
      "minzoom": 0,
      "maxzoom": 12,
      "attribution": "Satellite imagery: VersaTiles sources — https://versatiles.org/sources/"
    }
  },
  "layers": [
    {
      "id": "satellite",
      "type": "raster",
      "source": "versatiles-satellite",
      "minzoom": 0,
      "maxzoom": 22,
      "paint": {"raster-resampling": "linear"}
    }
  ]
}
""".trimIndent()

class IumrahMapController internal constructor(
    internal val mapView: MapView,
) {
    var map: MapLibreMap? by mutableStateOf(null)
        internal set

    var styleEpoch: Int by mutableIntStateOf(0)
        internal set

    private var appliedStyle: IumrahMapStyle? = null

    fun setStyle(style: IumrahMapStyle, force: Boolean = false) {
        val map = map ?: return
        if (!force && appliedStyle == style && styleEpoch > 0) return
        appliedStyle = style
        val builder = when (style) {
            IumrahMapStyle.STANDARD -> Style.Builder().fromUri(IUMRAH_STANDARD_MAP_STYLE)
            IumrahMapStyle.SATELLITE -> Style.Builder().fromJson(IUMRAH_SATELLITE_MAP_STYLE_JSON)
        }
        map.setStyle(builder) {
            styleEpoch += 1
        }
    }
}

@Composable
fun rememberIumrahMapController(): IumrahMapController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        IumrahMapController(MapView(context).apply { onCreate(null) })
    }

    DisposableEffect(controller, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var started = false
        var resumed = false

        fun startIfNeeded() {
            if (!started) {
                controller.mapView.onStart()
                started = true
            }
        }
        fun resumeIfNeeded() {
            startIfNeeded()
            if (!resumed) {
                controller.mapView.onResume()
                resumed = true
            }
        }
        fun pauseIfNeeded() {
            if (resumed) {
                controller.mapView.onPause()
                resumed = false
            }
        }
        fun stopIfNeeded() {
            pauseIfNeeded()
            if (started) {
                controller.mapView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startIfNeeded()
                Lifecycle.Event.ON_RESUME -> resumeIfNeeded()
                Lifecycle.Event.ON_PAUSE -> pauseIfNeeded()
                Lifecycle.Event.ON_STOP -> stopIfNeeded()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startIfNeeded()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeIfNeeded()

        controller.mapView.getMapAsync { map ->
            controller.map = map
        }

        onDispose {
            lifecycle.removeObserver(observer)
            stopIfNeeded()
            controller.mapView.onDestroy()
            controller.map = null
        }
    }

    return controller
}

@Composable
fun IumrahMapLibreView(
    controller: IumrahMapController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { controller.mapView },
        modifier = modifier,
    )
}
