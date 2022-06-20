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

        //        Hello.Toast(this, "hello Xerneas")

        val t1 = findViewById<View>(R.id.tv) as TextView
        t1.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}