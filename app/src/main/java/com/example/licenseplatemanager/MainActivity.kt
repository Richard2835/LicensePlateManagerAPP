package com.example.licenseplatemanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.licenseplatemanager.databinding.ActivityMainBinding
import com.example.licenseplatemanager.databinding.ContentMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ContentMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ContentMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.buttonId.setOnClickListener {
            startActivity(Intent(this,Login_page::class.java))
            finish()
        }


    }
}