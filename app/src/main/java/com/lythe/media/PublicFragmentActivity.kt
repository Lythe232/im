package com.lythe.media

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.lythe.media.ui.chats.ChatSettingFragment

class PublicFragmentActivity : AppCompatActivity() {

    companion object {
        const val KEY_FRAGMENT_TYPE = "fragment_type"
        const val TYPE_CHAT_FRIEND_SETTING = 1
        const val TYPE_CHAT_SEARCH = 2
        const val TYPE_CHAT_REPORT = 3
    }

    private lateinit var btnBack : ImageButton
    private lateinit var tvTitle : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_public_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initTopBar()
        val fragmentType = intent.getIntExtra(KEY_FRAGMENT_TYPE, -1)
        if(fragmentType != -1) {
            showFragmentByType(fragmentType)
        } else {
            finish()
        }
    }

    private fun initTopBar() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)

        btnBack.setOnClickListener { finish() }
    }
    private fun showFragmentByType(type: Int) {
        val fragment : Fragment = when(type) {
            TYPE_CHAT_FRIEND_SETTING -> {
                tvTitle.text = "聊天设置"
                ChatSettingFragment()
            }
            else -> {
                finish()
                return
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}