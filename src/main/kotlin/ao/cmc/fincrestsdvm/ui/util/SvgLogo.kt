package ao.cmc.fincrestsdvm.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.kitfox.svg.SVGUniverse
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream

/**
 * Compose Desktop has no built-in SVG support, so the brand logos (bundled
 * under src/main/resources/branding) are rasterized at runtime via
 * svgSalamander instead of being pre-exported to fixed-size PNGs — keeps
 * them sharp at whatever size each screen displays them at.
 */
private fun rasterizeSvgResource(resourcePath: String, widthPx: Int, heightPx: Int): ImageBitmap {
    val bytes = object {}.javaClass.getResourceAsStream(resourcePath)?.readBytes()
        ?: error("SVG resource not found on classpath: $resourcePath")
    val universe = SVGUniverse()
    val uri = universe.loadSVG(ByteArrayInputStream(bytes), resourcePath)
    val diagram = universe.getDiagram(uri)

    val image = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.scale(widthPx / diagram.width.toDouble(), heightPx / diagram.height.toDouble())
    diagram.render(g)
    g.dispose()
    return image.toComposeImageBitmap()
}

@Composable
fun SvgLogo(
    resourcePath: String,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    // Oversampled 3x so the raster stays crisp on high-DPI displays.
    val widthPx = with(density) { (width.toPx() * 3).toInt().coerceAtLeast(1) }
    val heightPx = with(density) { (height.toPx() * 3).toInt().coerceAtLeast(1) }
    val bitmap = remember(resourcePath, widthPx, heightPx) {
        rasterizeSvgResource(resourcePath, widthPx, heightPx)
    }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier.width(width).height(height)
    )
}
