package com.huddle01.kotlin_client_example_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.huddle01.kotlin_client.live_data.store.RoomStore
import com.huddle01.kotlin_client.utils.PeerConnectionUtils
import com.huddle01.kotlin_client_example_app.databinding.ActivityMeetingRoomBinding
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber

class MeetingRoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMeetingRoomBinding
    private lateinit var store: RoomStore
    private var isPermissionGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.trans_bg)
        binding = ActivityMeetingRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val huddleClient = (applicationContext as Application).huddleClient
        checkPermissions()
        store = huddleClient.localPeer.store
        binding.roomStore = store
        binding.lifecycleOwner = this
        binding.bottomNavigation.itemIconTintList = null

        val surfaceView: SurfaceViewRenderer = binding.localView
        val scrollableContainer: LinearLayout = binding.scrollableContainer
        "meeting roomId: ${huddleClient.roomId} 🆔".also { binding.toolbarTitle.text = it }

        var peerIds = store.peers.value?.allPeers ?: emptyList()
        fun updateScrollableContainer() {
            scrollableContainer.removeAllViews()

            val verticalLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val localViewLayoutParams = LinearLayout.LayoutParams(
                1000,
                500,
                1f
            ).apply {
                setMargins(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }
            val peerIdLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }

            val peerIdTextColor = resources.getColor(android.R.color.holo_orange_light, null)

            for (peer in peerIds) {
                val verticalLayout = LinearLayout(scrollableContainer.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = verticalLayoutParams
                }

                if (peer.consumers.isNotEmpty()) {
                    peer.consumers.values.forEach { consumer ->
                        if (consumer.kind == "video") {
                            val consumerVideoView =
                                SurfaceViewRenderer(scrollableContainer.context).apply {
                                    layoutParams = localViewLayoutParams
                                    elevation = 25f
                                    init(PeerConnectionUtils.eglContext, null)
                                    setMirror(true)
                                    val track: VideoTrack? = consumer.track as VideoTrack?
                                    track?.addSink(this)
                                }
                            verticalLayout.addView(consumerVideoView)
                        }
                    }
                }

                val peerIdTextView = TextView(scrollableContainer.context).apply {
                    layoutParams = peerIdLayoutParams
                    text = peer.peerId
                    textSize = 20f
                    gravity = android.view.Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(peerIdTextColor)
                }

                verticalLayout.addView(peerIdTextView)
                scrollableContainer.addView(verticalLayout)
            }
        }

        updateScrollableContainer()

        store.peers.observe(this) {
            peerIds = it.allPeers
            updateScrollableContainer()
        }

        var isMicOn = false
        var isCamOn = false

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.mic -> {
                    Timber.i("Mic Btn Pressed")
                    lifecycleScope.launch {
                        if (isMicOn) {
                            huddleClient.localPeer.disableAudio()
                            item.setIcon(R.drawable.icon_mic_off)
                        } else {
                            huddleClient.localPeer.enableAudio()
                            item.setIcon(R.drawable.icon_mic_on)
                        }
                        isMicOn = !isMicOn
                    }
                    true
                }

                R.id.cam -> {
                    Timber.i("Cam Btn Pressed")
                    lifecycleScope.launch {
                        if (isCamOn) {
                            huddleClient.localPeer.disableVideo(surfaceView)
                            item.setIcon(R.drawable.icon_video_off)
                        } else {
                            huddleClient.localPeer.enableVideo(surfaceView)
                            item.setIcon(R.drawable.icon_video_on)
                        }
                        isCamOn = !isCamOn
                    }
                    true
                }

                R.id.leave -> {
                    Timber.i("Leave Btn Pressed")
                    lifecycleScope.launch {
                        if (huddleClient.localPeer.role == "host") {
                            huddleClient.closeRoom()
                        } else {
                            huddleClient.leaveRoom()
                        }
                    }
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

    }


    private fun checkPermissions() {
        val permissions = listOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissions.isNotEmpty()) {
            requestPermissions.launch(permissions.toTypedArray())
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions.all { it.value }
        val message = if (isPermissionGranted) "Permission Granted" else "Permission Denied"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
