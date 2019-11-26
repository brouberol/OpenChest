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
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var mLight: Sensor
    private lateinit var mPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager

    private val activationLux = 2
    private val countDownDurationMs = 10000L
    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        mPlayer = MediaPlayer.create(this, R.raw.zeldachest)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Once the chest button has been pressed, start the countdown
        // after which the light sensor will be registered
        chestButton.setOnClickListener {
            mLight.also {
                // Prevent from playing the sound twice if your hand shakes
                if (!running) {
                    println("The chest has been pressed, starting countdown!")
                    running = true
                    countDownAndStart()
                }
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    // Count down, and register the light sensor at the end
    fun countDownAndStart() {
        val countdown = object : CountDownTimer(countDownDurationMs, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                instructionText.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                instructionText.text = ""
                println("Countdown done")
                registerLightSensor()
            }

        }
        instructionText.text = "${countDownDurationMs / 1000}"
        countdown.start()
    }

    fun registerLightSensor() {
        println("Registering light sensor")
        sensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun unregiserLightSensor() {
        println("Unregistering light sensor")
        sensorManager.unregisterListener(this)
    }

    // Play the sound at almost max volume
    fun playSound() {
        val maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        println("Playing sound at near max volume (${maxvolume - 1})")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxvolume - 1, AudioManager.FLAG_PLAY_SOUND)
        mPlayer.setOnCompletionListener {
            println("Sound is over.")
            unregiserLightSensor()
            turnLightsOff()
            instructionText.text = getString(R.string.press_on_the_chest_to_start)
            running = false
        }
        mPlayer.start()
    }

    // Event handler executed when the light sensor detects a change
    override fun onSensorChanged(event: SensorEvent) {
        val lux = event.values[0]
        println("Lux: ${lux}")
        if (lux >= activationLux && !mPlayer.isPlaying) {
            turnLightsOn()
            playSound()
        }
    }

    // Turn on the LED, if detected
    fun turnLightsOn() {
        if (cameraManager.cameraIdList.firstOrNull() != null) {
            println("Turning LED on")
            cameraManager.setTorchMode(cameraManager.cameraIdList[0], true)
        }

    }

    // Turn off the LED, if detected
    fun turnLightsOff() {
        if (cameraManager.cameraIdList.firstOrNull() != null) {
            println("Turning LED off")
            cameraManager.setTorchMode(cameraManager.cameraIdList[0], true)
        }
    }

    // Unregister the light sensor when the app goes dormant
    override fun onPause() {
        super.onPause()
        unregiserLightSensor()
    }
}
