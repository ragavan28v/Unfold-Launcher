package com.ragavan.unfold

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import com.unfold.core.ui.components.hud.HudMediaManager
import androidx.compose.ui.graphics.asImageBitmap

class UnfoldNotificationListenerService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updatePlaybackProgress()
            handler.postDelayed(this, 1000)
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        registerControllers(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        // Listen to control calls from the HUD UI
        HudMediaManager.onMediaControlListener = { keyCode ->
            val controller = activeController
            if (controller != null) {
                val time = android.os.SystemClock.uptimeMillis()
                controller.dispatchMediaButtonEvent(android.view.KeyEvent(time, time, android.view.KeyEvent.ACTION_DOWN, keyCode, 0))
                controller.dispatchMediaButtonEvent(android.view.KeyEvent(time, time, android.view.KeyEvent.ACTION_UP, keyCode, 0))
            } else {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                val time = android.os.SystemClock.uptimeMillis()
                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(time, time, android.view.KeyEvent.ACTION_DOWN, keyCode, 0))
                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(time, time, android.view.KeyEvent.ACTION_UP, keyCode, 0))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        HudMediaManager.onMediaControlListener = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val component = ComponentName(this, UnfoldNotificationListenerService::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, component)
            val controllers = mediaSessionManager.getActiveSessions(component)
            registerControllers(controllers)
            handler.post(updateRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        handler.removeCallbacks(updateRunnable)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMetadata()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata()
        }
    }

    private fun registerControllers(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(callback)
        activeController = controllers?.firstOrNull()
        activeController?.registerCallback(callback)
        updateMetadata()
    }

    private fun updateMetadata() {
        val controller = activeController
        if (controller != null) {
            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            val albumArt = artBitmap?.asImageBitmap()

            HudMediaManager.updateState(
                HudMediaManager.MusicState(
                    title = title,
                    artist = artist.uppercase(),
                    packageName = controller.packageName,
                    isPlaying = isPlaying,
                    position = playbackState?.position ?: 0L,
                    duration = duration,
                    albumArt = albumArt
                )
            )
        } else {
            HudMediaManager.updateState(HudMediaManager.MusicState())
        }
    }

    private fun updatePlaybackProgress() {
        val controller = activeController ?: return
        val playbackState = controller.playbackState ?: return
        if (playbackState.state == PlaybackState.STATE_PLAYING) {
            val currentPos = playbackState.position
            val lastUpdate = playbackState.lastPositionUpdateTime
            val now = android.os.SystemClock.elapsedRealtime()
            val interpolatedPosition = if (lastUpdate > 0) {
                currentPos + (now - lastUpdate)
            } else {
                currentPos
            }
            val currentState = HudMediaManager.musicState.value
            HudMediaManager.updateState(
                currentState.copy(
                    position = interpolatedPosition
                )
            )
        }
    }

    companion object {
        var activeController: MediaController? = null
    }
}
