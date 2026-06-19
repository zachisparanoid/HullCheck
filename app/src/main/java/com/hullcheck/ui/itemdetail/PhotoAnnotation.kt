package com.hullcheck.ui.itemdetail

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hullcheck.R
import com.hullcheck.data.model.Photo
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

private class AnnotationStroke(val color: Color) {
    val points = mutableStateListOf<Offset>()
}

private val ANNOTATION_COLORS = listOf(
    Color(0xFFFF3B30), // red
    Color(0xFFFFD60A), // yellow
    Color(0xFF30D158), // green
    Color(0xFF0A84FF), // blue
    Color.White,
)

private const val VIEW_STROKE_WIDTH = 9f

/**
 * Freehand annotation over a photo. Records strokes in view coordinates, then composites them
 * onto a mutable copy of the source bitmap (mapping view → pixel space) and hands the result back.
 */
@Composable
fun PhotoAnnotationScreen(
    photo: Photo,
    onSaved: (android.graphics.Bitmap) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var source by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var color by remember { mutableStateOf(ANNOTATION_COLORS.first()) }
    val strokes = remember { mutableStateListOf<AnnotationStroke>() }

    LaunchedEffect(photo.id) {
        val bmp = decodeBounded(File(photo.filePath), 2000)
        source = bmp
        image = bmp?.asImageBitmap()
    }

    fun mapping(): Mapping? {
        val src = source ?: return null
        if (canvasSize.width == 0 || canvasSize.height == 0) return null
        val scale = min(canvasSize.width.toFloat() / src.width, canvasSize.height.toFloat() / src.height)
        val dstW = src.width * scale
        val dstH = src.height * scale
        return Mapping(scale, (canvasSize.width - dstW) / 2f, (canvasSize.height - dstH) / 2f, dstW, dstH)
    }

    fun composite() {
        val src = source ?: return
        val map = mapping() ?: return
        val mutable = src.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutable)
        strokes.forEach { stroke ->
            val paint = android.graphics.Paint().apply {
                this.color = stroke.color.toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = VIEW_STROKE_WIDTH / map.scale
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }
            val path = android.graphics.Path()
            stroke.points.forEachIndexed { i, p ->
                val bx = (p.x - map.left) / map.scale
                val by = (p.y - map.top) / map.scale
                if (i == 0) path.moveTo(bx, by) else path.lineTo(bx, by)
            }
            canvas.drawPath(path, paint)
        }
        onSaved(mutable)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val img = image
        if (img != null) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                strokes.add(AnnotationStroke(color).also { it.points.add(offset) })
                            },
                            onDrag = { change, _ ->
                                strokes.lastOrNull()?.points?.add(change.position)
                            },
                        )
                    },
            ) {
                val scale = min(size.width / img.width, size.height / img.height)
                val dstW = (img.width * scale).roundToInt()
                val dstH = (img.height * scale).roundToInt()
                val left = ((size.width - dstW) / 2f).roundToInt()
                val top = ((size.height - dstH) / 2f).roundToInt()
                drawImage(
                    image = img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(dstW, dstH),
                )
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, stroke.color, style = Stroke(width = VIEW_STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }

        // Top actions
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
            Row {
                IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.annotation_undo), tint = Color.White)
                }
                IconButton(onClick = { composite() }) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save), tint = Color.White)
                }
            }
        }

        // Color palette
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ANNOTATION_COLORS.forEach { swatch ->
                Box(
                    Modifier
                        .size(34.dp)
                        .background(swatch, CircleShape)
                        .border(if (swatch == color) 3.dp else 1.dp, Color.White, CircleShape)
                        .clickable { color = swatch },
                )
            }
        }
    }
}

private class Mapping(val scale: Float, val left: Float, val top: Float, val dstW: Float, val dstH: Float)

private fun decodeBounded(file: File, maxEdge: Int): android.graphics.Bitmap? {
    if (!file.exists()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    }.getOrNull()
}
