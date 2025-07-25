package com.fooddiary.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.fooddiary.R

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(initialValue = 0.8f) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = LinearOutSlowInEasing
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(R.mipmap.ic_launcher2)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value)
        )
    }
}