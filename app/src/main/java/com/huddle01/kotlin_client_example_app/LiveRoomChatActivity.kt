package com.huddle01.kotlin_client_example_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.huddle01.kotlin_client.live_data.store.RoomStore
import com.huddle01.kotlin_client_example_app.databinding.ActivityLiveRoomChatBinding
import kotlinx.coroutines.launch
import timber.log.Timber

class LiveRoomChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveRoomChatBinding
    private lateinit var store: RoomStore
    private var isPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeUI()
        initializeHuddleClient()
        observeRoomData()
        setButtonListeners()
        requestPermissionsIfNeeded()
    }

    private fun initializeUI() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        binding = ActivityLiveRoomChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initializeHuddleClient() {
        val huddleClient = (applicationContext as Application).huddleClient
        store = huddleClient.localPeer.store
        binding.roomStore = store
        binding.lifecycleOwner = this

        when {
            huddleClient.localPeer.role == "host"  || huddleClient.localPeer.role == "coHost" -> {
                when {
                    intent.getBooleanExtra("isMicOn", false) -> {
                        lifecycleScope.launch { huddleClient.localPeer.enableAudio() }
                    }
                    intent.getBooleanExtra("isCamOn", false) -> {
                        lifecycleScope.launch { huddleClient.localPeer.enableVideo(binding.camView) }
                    }
                }
            }
            else -> {
                binding.btnMic.visibility = View.GONE
                binding.btnCam.visibility = View.GONE
                binding.btnSwitchCam.visibility = View.GONE

            }
        }
    }

    private fun observeRoomData() {
        store.peers.observe(this) {
            val peerCount = it.allPeers.count()
            binding.peersCount.text = peerCount.toString()
        }
    }

    private fun setButtonListeners() {
        binding.btnMic.setOnClickListener { handleMicToggle() }
        binding.btnCam.setOnClickListener { handleCamToggle() }
        binding.btnSwitchCam.setOnClickListener { switchCamera() }
        binding.btnLeave.setOnClickListener { leaveRoom() }
    }

    private fun handleMicToggle() {
        Timber.i("Mic Button Pressed")
        val huddleClient = (applicationContext as Application).huddleClient
        lifecycleScope.launch {
            val isMicOn = intent.getBooleanExtra("isMicOn", false)
            if (isMicOn) {
                huddleClient.localPeer.disableAudio()
                binding.btnMic.setImageResource(R.drawable.icon_mic_off)
            } else {
                huddleClient.localPeer.enableAudio()
                binding.btnMic.setImageResource(R.drawable.icon_mic_on)
            }
            intent.putExtra("isMicOn", !isMicOn)
        }
    }

    private fun handleCamToggle() {
        Timber.i("Camera Button Pressed")
        val huddleClient = (applicationContext as Application).huddleClient
        lifecycleScope.launch {
            val isCamOn = intent.getBooleanExtra("isCamOn", false)
            if (isCamOn) {
                huddleClient.localPeer.disableVideo(binding.camView)
                binding.btnCam.setImageResource(R.drawable.icon_video_off)
            } else {
                huddleClient.localPeer.enableVideo(binding.camView)
                binding.btnCam.setImageResource(R.drawable.icon_video_on)
            }
            intent.putExtra("isCamOn", !isCamOn)
        }
    }

    private fun switchCamera() {
        (applicationContext as Application).huddleClient.localPeer.changeCam()
    }

    private fun leaveRoom() {
        Timber.i("Leave Button Pressed")
        val huddleClient = (applicationContext as Application).huddleClient
        lifecycleScope.launch {
            if (huddleClient.localPeer.role == "host") {
                huddleClient.closeRoom()
            } else {
                huddleClient.leaveRoom()
            }
        }
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = listOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions.all { it.value }
        val message = if (isPermissionGranted) "Permission Granted" else "Permission Denied"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
