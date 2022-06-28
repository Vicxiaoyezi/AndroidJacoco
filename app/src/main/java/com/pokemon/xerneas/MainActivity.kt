package com.pokemon.xerneas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val t1 = findViewById<View>(R.id.button_first) as TextView
        t1.setOnClickListener {
            startActivity(Intent(this, FirstActivity::class.java))
        }

        val t2 = findViewById<View>(R.id.button_second) as TextView
        t2.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}