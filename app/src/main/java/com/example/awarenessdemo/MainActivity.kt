package com.example.awarenessdemo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.awarenessdemo.extensions.asHtml
import com.example.awarenessdemo.extensions.stateString
import com.example.awarenessdemo.extensions.toDateTimeFormat
import com.example.awarenessdemo.extensions.toString
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.FenceClient
import com.google.android.gms.awareness.SnapshotClient
import com.google.android.gms.awareness.fence.*
import com.google.android.gms.awareness.state.HeadphoneState
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val PHONES_C = "PhonesC"
        const val PHONES_D = "PhonesD"
        const val USER_W = "UserW"
        const val USER_I = "UserI"
        const val USER_S = "UserS"
        const val USER_CAR = "UserCar"
        const val USER_S_W = "UserStopW"
        const val FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION"
    }

    private lateinit var fenceClient: FenceClient
    private lateinit var snapshotClient: SnapshotClient

    private var fenceReceiver = FenceReceiver()
    private var isTrackingNow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        registerReceiver(fenceReceiver, IntentFilter(FENCE_RECEIVER_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fenceReceiver)
    }


    private fun init() {
        fenceClient = Awareness.getFenceClient(this)
        snapshotClient = Awareness.getSnapshotClient(this)

        initListeners()

        snapshotCheckHeadphones()
        snapshotGetLocation()
        snapShotGetUserActivity()
    }

    private fun changeTrackingState() {
        isTrackingNow = !isTrackingNow
        if (isTrackingNow) {
            initFence()
        } else {
            unregisterFence()
        }
    }

    private fun unregisterFence() {
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(USER_CAR).build()
        )
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(PHONES_C).build()
        )
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(PHONES_D).build()
        )
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(USER_I).build()
        )
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(USER_S).build()
        )
        fenceClient.updateFences(
                FenceUpdateRequest.Builder()
                    .removeFence(USER_W).build()
                )
        fenceClient.updateFences(
            FenceUpdateRequest.Builder()
                .removeFence(USER_S_W).build()
        )
        fenceLogTv.text = ""
        fenceLogTv.animate().alpha(0f).start()
        fenceSubtitle.animate().alpha(0f).start()
        fenceParametersScrollContainer.animate().alpha(1f)
            .withStartAction { fenceParametersScrollContainer.isVisible = true }
        fenceTrackingBt.setText(R.string.start_tracking)
    }

    private fun initFence() {
        val selectedFenceParameters = getSelectedParameters()
        if (selectedFenceParameters.fenceList.isEmpty()) {
            toast("Please, select at least one fence")
            isTrackingNow = false
            return
        }

        fenceSubtitle.text = getString(
            R.string.fence_subtitle,
            selectedFenceParameters.parametersNames.joinToString(separator = ", ")
        )

        fenceLogTv.animate().alpha(1f).start()
        fenceSubtitle.animate().alpha(1f).start()
        fenceParametersScrollContainer.animate().alpha(0f)
            .withEndAction { fenceParametersScrollContainer.isVisible = false }
        fenceTrackingBt.setText(R.string.stop_tracking)
    }

    private fun getSelectedParameters(): SelectedFenceParameters {
        val listNames = mutableListOf<String>()
        val listFences = mutableListOf<AwarenessFence>()
        val intent = Intent(FENCE_RECEIVER_ACTION)
        val mPendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        with(fenceHeadphonesConnect) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(HeadphoneFence.during(HeadphoneState.PLUGGED_IN))

                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        PHONES_C,
                        HeadphoneFence.during(HeadphoneState.PLUGGED_IN),
                        mPendingIntent
                    ).build()
                )

            }
        }
        with(fenceHeadphonesDisconnect) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(HeadphoneFence.during(HeadphoneState.UNPLUGGED))
                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        PHONES_D,
                        HeadphoneFence.during(HeadphoneState.UNPLUGGED),
                        mPendingIntent
                    ).build()
                )

            }
        }
        with(fenceUserIdle) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(DetectedActivityFence.during(DetectedActivityFence.STILL))

                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        USER_I,
                        DetectedActivityFence.during(DetectedActivityFence.STILL),
                        mPendingIntent
                    ).build()
                )

            }
        }
        with(fenceUserStartWalking) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(DetectedActivityFence.starting(DetectedActivityFence.WALKING))
                listFences.add(DetectedActivityFence.starting(DetectedActivityFence.ON_FOOT))
                listFences.add(DetectedActivityFence.starting(DetectedActivityFence.RUNNING))
                val fences = mutableListOf<AwarenessFence>()
                fences.add(DetectedActivityFence.starting(DetectedActivityFence.WALKING))
                fences.add(DetectedActivityFence.starting(DetectedActivityFence.ON_FOOT))
                fences.add(DetectedActivityFence.starting(DetectedActivityFence.RUNNING))
                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        USER_S_W,
                        AwarenessFence.and(fences),
                        mPendingIntent
                    ).build()
                )

            }
        }
        with(fenceUserWalking) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(DetectedActivityFence.during(DetectedActivityFence.WALKING))
                listFences.add(DetectedActivityFence.during(DetectedActivityFence.ON_FOOT))
                listFences.add(DetectedActivityFence.during(DetectedActivityFence.RUNNING))
                val fences = mutableListOf<AwarenessFence>()
                fences.add(DetectedActivityFence.during(DetectedActivityFence.WALKING))
                fences.add(DetectedActivityFence.during(DetectedActivityFence.ON_FOOT))
                fences.add(DetectedActivityFence.during(DetectedActivityFence.RUNNING))
                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        USER_W,
                        AwarenessFence.and(fences),
                        mPendingIntent
                    ).build()
                )
            }
        }
        with(fenceUserStopWalking) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(DetectedActivityFence.stopping(DetectedActivityFence.WALKING))
                listFences.add(DetectedActivityFence.stopping(DetectedActivityFence.ON_FOOT))
                listFences.add(DetectedActivityFence.stopping(DetectedActivityFence.RUNNING))
                val fences = mutableListOf<AwarenessFence>()
                fences.add(DetectedActivityFence.stopping(DetectedActivityFence.WALKING))
                fences.add(DetectedActivityFence.stopping(DetectedActivityFence.ON_FOOT))
                fences.add(DetectedActivityFence.stopping(DetectedActivityFence.RUNNING))
                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        USER_S,
                        AwarenessFence.and(fences),
                        mPendingIntent
                    ).build()
                )
            }
        }
        with(fenceUserInCar) {
            if (isChecked) {
                listNames.add(text.toString())
                listFences.add(DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE))
                listFences.add(DetectedActivityFence.starting(DetectedActivityFence.IN_VEHICLE))
                listFences.add(DetectedActivityFence.stopping(DetectedActivityFence.IN_VEHICLE))
                val fences = mutableListOf<AwarenessFence>()
                fences.add(DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE))
                fences.add(DetectedActivityFence.starting(DetectedActivityFence.IN_VEHICLE))
                fences.add(DetectedActivityFence.stopping(DetectedActivityFence.IN_VEHICLE))
                fenceClient.updateFences(
                    FenceUpdateRequest.Builder().addFence(
                        USER_CAR,
                        AwarenessFence.and(fences),
                        mPendingIntent
                    ).build()
                )
            }
        }
        this@MainActivity.fenceLogTv.append("Start tracking")
        return SelectedFenceParameters(
            parametersNames = listNames,
            fenceList = listFences
        )
    }

    private fun initListeners() {
        snapshotHeadphonesContainer.setOnClickListener { snapshotCheckHeadphones() }
        snapshotLocationContainer.setOnClickListener {
            snapshotGetLocation()
        }
        snapshotActivityContainer.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                snapShotGetUserActivity()
            } else {
                snapShotGetUserActivity()
            }
        }
        fenceTrackingBt.setOnClickListener {
            changeTrackingState()
        }
    }

    private fun snapshotCheckHeadphones() {
        snapshotClient.headphoneState
            .addOnSuccessListener {
                snapshotHeadphonesContainer.text = it.headphoneState.toString(this)
                snapshotHeadphonesContainer.setTextColor(ContextCompat.getColor(this, R.color.gray))
            }
            .addOnFailureListener {
                handleSnapshotError(snapshotHeadphonesContainer, it)
            }
    }

    @SuppressLint("MissingPermission")
    private fun snapshotGetLocation() {
        snapshotClient.location
            .addOnSuccessListener {
                with(it.location) {
                    snapshotLocationContainer.text = getString(
                        R.string.user_location,
                        latitude, longitude, altitude, accuracy
                    )
                }
                snapshotLocationContainer.setTextColor(ContextCompat.getColor(this, R.color.gray))
            }
            .addOnFailureListener {
                handleSnapshotError(snapshotLocationContainer, it)
            }
    }

    private fun snapShotGetUserActivity() {
        snapshotClient.detectedActivity
            .addOnSuccessListener {
                with(it.activityRecognitionResult) {
                    snapshotActivityContainer.text = getString(
                        R.string.user_activity,
                        mostProbableActivity.stateString(),
                        mostProbableActivity.confidence
                    )
                }
                snapshotActivityContainer.setTextColor(ContextCompat.getColor(this, R.color.gray))
            }
            .addOnFailureListener {
                handleSnapshotError(snapshotActivityContainer, it)
            }
    }

    private fun handleSnapshotError(view: TextView, error: Throwable) {
        with(view) {
            text = error.message
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
    }

    inner class FenceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val fenceState = FenceState.extract(intent)
           Log.d("key", fenceState.fenceKey)
            if (fenceState.fenceKey == PHONES_C) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Phones connected:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )
                        MediaPlayer.create(this@MainActivity, R.raw.sound).start()
                    }

                }
            }

            if (fenceState.fenceKey == PHONES_D) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Phones Disconnected:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )
                    }
                }
            }

            if (fenceState.fenceKey == USER_W) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Walking:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )

                    }
                }
            }
            if (fenceState.fenceKey == USER_S) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Stopped:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )

                    }
                }
            }
            if (fenceState.fenceKey == USER_I) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Idle:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )

                    }
                }
            }
            if (fenceState.fenceKey == USER_CAR) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Car:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )

                    }
                }
            }
            if (fenceState.fenceKey == USER_S_W) {
                when (fenceState.currentState) {
                    FenceState.TRUE -> {
                        this@MainActivity.fenceLogTv.append(
                            "<br><font color=red>Start Walking:</font> when: ${
                                Date(
                                    fenceState.lastFenceUpdateTimeMillis
                                ).toDateTimeFormat()
                            }".asHtml()
                        )

                    }
                }
            }
        }
    }
}