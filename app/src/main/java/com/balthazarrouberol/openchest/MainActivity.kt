package com.balthazarrouberol.openchest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var mLight: Sensor
    private lateinit var mPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager

    private val activationLux = 5
    private val activationPeriodMs = 100
    private var isActivated = false
    private var isBeingActivated = false
    private var startActivationMs = 0L
    private var done = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        mPlayer = MediaPlayer.create(this, R.raw.zeldachest)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Don't do anything for now
    }

    // This function is in charge of setting the isActivated boolean
    // attribute to true whenever the ambiant lux measurement has been
    // greater than a given threshold for more than a given amount of time
    fun activateAfter(currentLux: Float) {
        println("Lux: ${currentLux}")
        if (!isActivated && currentLux >= activationLux) {
            if (isBeingActivated == false) {
                isBeingActivated = true
                startActivationMs = System.currentTimeMillis()
                println("Starting activation at ${startActivationMs}")
            } else {
                println("Checking if fully activated")
                val now = System.currentTimeMillis()
                if (now - startActivationMs >= activationPeriodMs) {
                    println("Activated!")
                    isActivated = true
                    isBeingActivated = false
                }
            }
        }
    }

    fun playSound() {
        val maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxvolume - 1, AudioManager.FLAG_PLAY_SOUND)
        mPlayer.setOnCompletionListener {
            done = true
        }
        mPlayer.start()
    }

    fun turnLightsOn() {
        // println(cameraManager.getCameraIdList())
    }

    override fun onSensorChanged(event: SensorEvent) {
        val lux = event.values[0]
        activateAfter(lux)
        if (isActivated && !done && !mPlayer.isPlaying) {
            turnLightsOn()
            playSound()
        }
    }

    override fun onResume() {
        super.onResume()
        mLight.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
