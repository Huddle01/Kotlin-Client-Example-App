package com.huddle01.kotlin_client_example_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.huddle01.kotlin_client_example_app.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.trans_bg)
        setContentView(binding.root)
        val huddleClient = (applicationContext as Application).huddleClient

        binding.btnConnect.setOnClickListener {
            val roomId = binding.roomId.text.toString().trim()
            val token = binding.token.text.toString().trim()

            lifecycleScope.launch {
                try {
                    huddleClient.joinRoom(roomId, token)
                    val intent = Intent(this@HomeActivity, MeetingRoomActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@HomeActivity, "Failed to join the room: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}