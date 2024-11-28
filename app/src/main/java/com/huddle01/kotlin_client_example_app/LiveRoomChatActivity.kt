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
import com.huddle01.kotlin_client.HuddleClient
import com.huddle01.kotlin_client.live_data.store.HuddleStore
import com.huddle01.kotlin_client.live_data.store.models.Peer
import com.huddle01.kotlin_client.models.enum_class.RoomStates
import com.huddle01.kotlin_client.utils.PeerConnectionUtils
import com.huddle01.kotlin_client_example_app.databinding.ActivityLiveRoomChatBinding
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import timber.log.Timber

class LiveRoomChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveRoomChatBinding
    private lateinit var store: HuddleStore
    private lateinit var peerIds: List<Peer>
    private lateinit var huddleClient: HuddleClient

    private var isMicOn: Boolean = false
    private var isCamOn: Boolean = false
    private var isPermissionGranted: Boolean = false

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
        huddleClient = (applicationContext as Application).huddleClient
        store = huddleClient.localPeer.store
        binding.huddleStore = store
        binding.lifecycleOwner = this
        peerIds = store.peers.value?.allPeers ?: emptyList()

        if (huddleClient.localPeer.role in listOf("host", "coHost")) {
            lifecycleScope.launch {
                huddleClient.localPeer.enableAudio()
                huddleClient.localPeer.enableVideo(binding.camView)
            }
        } else {
            binding.btnMic.visibility = View.GONE
            binding.btnCam.visibility = View.GONE
            binding.btnSwitchCam.visibility = View.GONE
        }
    }

    private fun observeRoomData() {
        store.peers.observe(this) { it ->
            peerIds = it.allPeers
            "${peerIds.count()}".also {
                binding.peersCount.text = it
            }
            val myPeer = store.me.value
            if (myPeer?.peerId == "host") return@observe
            val hostTrack =
                myPeer?.myConsumedTracks?.values?.firstOrNull { it.kind() == "video" } as? VideoTrack
            if (hostTrack != null) {
                binding.camView.release()
                binding.camView.init(PeerConnectionUtils.eglContext, null)
                hostTrack.addSink(binding.camView)
            }
        }
        store.roomInfo.observe(this) { roomInfo ->
            if (roomInfo.connectionState == RoomStates.CLOSED) {
                val intent: Intent = Intent(
                    this,
                    HomeActivity::class.java
                )
                finishAffinity()
                startActivity(intent)
            }
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
            if (isMicOn) {
                huddleClient.localPeer.muteMic()
                binding.btnMic.setImageResource(R.drawable.icon_mic_off)
            } else {
                huddleClient.localPeer.unMuteMic()
                binding.btnMic.setImageResource(R.drawable.icon_mic_on)
            }
            isMicOn = !isMicOn
        }
    }

    private fun handleCamToggle() {
        Timber.i("Camera Button Pressed")
        val huddleClient = (applicationContext as Application).huddleClient
        lifecycleScope.launch {
            if (isCamOn) {
                huddleClient.localPeer.disableVideo(binding.camView)
                binding.btnCam.setImageResource(R.drawable.icon_video_off)
            } else {
                huddleClient.localPeer.enableVideo(binding.camView)
                binding.btnCam.setImageResource(R.drawable.icon_video_on)
            }
            isCamOn = !isCamOn
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

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            huddleClient.leaveRoom()
        }
    }
}