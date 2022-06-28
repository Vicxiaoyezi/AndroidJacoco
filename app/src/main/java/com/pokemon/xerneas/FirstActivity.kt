package com.pokemon.xerneas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FirstActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        Hello.Toast(this, "hello Xerneas")
    }
}