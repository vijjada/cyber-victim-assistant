package com.cybervictim.assistant.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * ScreenDescriptorRegistry keeps track of the active screen descriptor.
 * Each screen publishes its descriptor via [TrackScreenDescriptor].
 */
object ScreenDescriptorRegistry {
    private val _currentDescriptor = mutableStateOf<ScreenDescriptor?>(null)
    val currentDescriptor: State<ScreenDescriptor?> = _currentDescriptor

    fun update(descriptor: ScreenDescriptor?) {
        _currentDescriptor.value = descriptor
    }
}

/**
 * Composable helper to publish a ScreenDescriptor while the screen is mounted.
 * Automatically clears the registry when unmounted.
 */
@Composable
fun TrackScreenDescriptor(descriptor: ScreenDescriptor) {
    DisposableEffect(descriptor) {
        ScreenDescriptorRegistry.update(descriptor)
        onDispose {
            if (ScreenDescriptorRegistry.currentDescriptor.value == descriptor) {
                ScreenDescriptorRegistry.update(null)
            }
        }
    }
}
