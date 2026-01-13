package io.beldex.bchat.compose_utils.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.beldex.bchat.compose_utils.appColors
import java.io.File

@Composable
fun CroppedImageScreen(file: File, onCropped: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var rotationState by remember { mutableFloatStateOf(0f) }
    var cropRect by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset = Offset(offset.x + panChange.x * scale, offset.y + panChange.y * scale)
    }

    var cropMode by remember { mutableStateOf(false) }
    var cropVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = cropMode) {
        if (cropMode) {
            cropMode = false
            cropVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = rememberImagePainter(data = file),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = if (cropMode) 1f else scale,
                    scaleY = if (cropMode) 1f else scale,
                    translationX = if (cropMode) 0f else offset.x,
                    translationY = if (cropMode) 0f else offset.y,
                    rotationZ = rotationState
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (cropMode) return@detectTransformGestures

                        scale *= zoom
                        offset = Offset(offset.x + pan.x * scale, offset.y + pan.y * scale)
                        rotationState += rotation
                    }
                }
                .clip(RectangleShape)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        if (!cropMode) return@detectTransformGestures

                        val x = (pan.x + offset.x) / size.width
                        val y = (pan.y + offset.y) / size.height
                        cropRect = cropRect.copy(right = x.coerceIn(0f, 1f), bottom = y.coerceIn(0f, 1f))
                    }
                }
                .then(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (cropMode) 1f else scale,
                            scaleY = if (cropMode) 1f else scale,
                            translationX = if (cropMode) 0f else offset.x,
                            translationY = if (cropMode) 0f else offset.y,
                            rotationZ = rotationState
                        )
//                        .clip(
//                            if (cropMode) {
//                                // Crop shape
//                                val scale = 1 / scale
//                                val scaleX = size.width / file.width
//                                val scaleY = size.height / file.height
//                                val minScale = minOf(scaleX, scaleY)
//                                val x = cropRect.x * size.width
//                                val y = cropRect.y * size.height
//                                val width = cropRect.width * size.width
//                                val height = cropRect.height * size.height
//                                val scaledWidth = width * scale
//                                val scaledHeight = height * scale
//                                val scaledX = x * scale
//                                val scaledY = y * scale
//                                val offsetX = (size.width - scaledWidth) / 2
//                                val offsetY = (size.height - scaledHeight) / 2
//                                RoundedCornerShape(
//                                    size = Size(
//                                        width = scaledWidth / minScale,
//                                        height = scaledHeight / minScale
//                                    ),
//                                    corner = CornerRadius.Zero
//                                ).offset(
//                                    x = (scaledX - offsetX) / minScale,
//                                    y = (scaledY - offsetY) / minScale
//                                )
//                            } else {
//                                // Full image shape
//                                RoundedCornerShape(corner = CornerSize(0))
//                            }
//                        )
                )
        )

        if (cropMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            val x = (pan.x + offset.x) / size.width
                            val y = (pan.y + offset.y) / size.height
//                            cropRect = cropRect.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f))
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner = CornerSize(0)))
            ) {
                // Draw crop rectangle
//                DrawRect(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(MaterialTheme.colorScheme.primary)
//                        .then(
//                            Modifier.graphicsLayer(
//                                scaleX = cropRect.width,
//                                scaleY = cropRect.height,
//                                translationX = cropRect.x,
//                                translationY = cropRect.y
//                            )
//                        )
//                )

                // Draw crop controls
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.primaryButtonColor)
                            .clickable { /* Rotate */ }
                    ) {
                        Icon(imageVector = Icons.Default.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.primaryButtonColor)
                            .clickable { /* Zoom in */ }
                    ) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.primaryButtonColor)
                            .clickable { /* Zoom out */ }
                    ) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.primaryButtonColor)
                            .clickable { cropMode = false }
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.primaryButtonColor)
                            .clickable { cropMode = false }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}