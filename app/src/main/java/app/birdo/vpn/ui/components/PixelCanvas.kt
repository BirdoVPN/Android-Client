package app.birdo.vpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Animated pixel canvas background matching the Windows client.
 *
 * Renders a full-screen grid of subtly twinkling white squares on a black
 * background. Each pixel fades in/out randomly at low alpha (0–0.15),
 * creating an ambient, alive feeling.
 *
 * Performance: Only ~0.1% of pixels change per frame. We use a flat IntArray
 * for alpha values (0-255 range mapped to 0-0.25 opacity) and redraw at
 * ~15 fps to keep GPU/CPU usage minimal.
 */
@Composable
fun PixelCanvas(
    modifier: Modifier = Modifier,
    pixelSize: Float = 20f,
    gapSize: Float = 1f,
) {
    // Pixel grid state — use a frame counter to trigger recomposition
    var frame by remember { mutableIntStateOf(0) }

    // Grid data — lazy-init on first composition
    val gridState = remember { PixelGridState() }

    // Animation loop at ~15 fps (66ms) — enough for subtle twinkling
    LaunchedEffect(Unit) {
        while (true) {
            delay(66L)
            gridState.tick()
            frame++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cellSize = pixelSize + gapSize

        // Initialize or resize grid if needed
        val cols = (canvasWidth / cellSize).toInt() + 1
        val rows = (canvasHeight / cellSize).toInt() + 1
        gridState.ensureSize(cols, rows)

        // Force read of frame to subscribe to changes
        @Suppress("UNUSED_VARIABLE") val f = frame

        // Draw pixels
        val pixelSizeObj = Size(pixelSize, pixelSize)
        for (row in 0 until gridState.rows) {
            for (col in 0 until gridState.cols) {
                val alpha = gridState.getAlpha(col, row)
                if (alpha > 0.005f) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = pixelSizeObj,
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

/**
 * Manages the pixel grid state efficiently using flat arrays.
 * Each pixel has a current alpha and a target alpha, and slowly
 * transitions between them.
 */
private class PixelGridState {
    var cols = 0
        private set
    var rows = 0
        private set

    // Current alpha (0f–0.25f) stored as Int (0–250) for memory efficiency
    private var currentAlpha = IntArray(0)
    // Target alpha
    private var targetAlpha = IntArray(0)
    // Speed (int, maps to 0.002–0.006)
    private var speed = IntArray(0)

    fun ensureSize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        cols = newCols
        rows = newRows
        val total = cols * rows
        currentAlpha = IntArray(total) { Random.nextInt(0, 10) } // 0-0.10 initial
        targetAlpha = IntArray(total) { Random.nextInt(0, 5) }   // some start with a target
        speed = IntArray(total) { 1 + Random.nextInt(3) }        // 1-3 for smoother transitions
    }

    fun tick() {
        val total = cols * rows
        if (total == 0) return

        for (i in 0 until total) {
            // ~0.3% chance of picking a new target per pixel per frame
            if (Random.nextFloat() < 0.003f) {
                targetAlpha[i] = Random.nextInt(0, 20) // 0–0.20 alpha range
            }

            // Ease toward target
            val cur = currentAlpha[i]
            val target = targetAlpha[i]
            val spd = speed[i]

            if (cur < target) {
                currentAlpha[i] = minOf(cur + spd, target)
            } else if (cur > target) {
                currentAlpha[i] = maxOf(cur - spd, target)
            }
        }
    }

    fun getAlpha(col: Int, row: Int): Float {
        val idx = row * cols + col
        if (idx < 0 || idx >= currentAlpha.size) return 0f
        // Map 0-20 → 0-0.20 alpha
        return currentAlpha[idx] / 100f
    }
}
