package com.anwesh.uiprojects.linkedverticallinetosquareview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.verticallinetosquareview.VerticalLineToSquareView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VerticalLineToSquareView.create(this)
    }
}
