package com.example.awarenessdemo.extensions

import android.content.Context
import com.google.android.gms.awareness.state.HeadphoneState
import com.example.awarenessdemo.R

fun HeadphoneState.toString(context: Context): String {
    return context.getString(
        R.string.headphones_state,
        if (state == HeadphoneState.PLUGGED_IN) {
            context.getString(R.string.headphones_state_connect)
        } else {
            context.getString(
                R.string.headphones_state_disconnected
            )
        }
    )
}