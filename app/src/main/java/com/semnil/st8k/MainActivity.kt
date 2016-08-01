package com.semnil.st8k

import android.media.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import org.joda.time.DateTime
import org.joda.time.Duration
import java.util.*
import kotlin.concurrent.*

class MainActivity : AppCompatActivity() {

    val self : MainActivity = this

    lateinit var countText : TextView
    lateinit var intervalNumber : NumberPicker
    lateinit var repeatSwitch : Switch
    lateinit var skipCommandSwitch : Switch
    lateinit var humanizeSwitch : Switch
    lateinit var startButton : Button

    lateinit var soundPool : SoundPool
    var beepId: Int = 0
    var standId: Int = 0
    var readyId: Int = 0

    var soundActive: Boolean = false
    var countActive: Boolean = false
    var beepTimer: Timer = Timer()
    val handler : Handler = Handler()

    lateinit var countStartDate : DateTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countText = findViewById(R.id.count_text) as TextView
        intervalNumber = findViewById(R.id.interval_number) as NumberPicker
        intervalNumber.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        intervalNumber.minValue = 0
        intervalNumber.maxValue = 30
        intervalNumber.value = 3
        repeatSwitch = findViewById(R.id.repeat_switch) as Switch
        skipCommandSwitch = findViewById(R.id.skip_command_switch) as Switch
        humanizeSwitch = findViewById(R.id.humanize_switch) as Switch
        startButton = findViewById(R.id.start_button) as Button

        soundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 0)
        standId = soundPool.load(this, R.raw.stand_by, 1)
        readyId = soundPool.load(this, R.raw.are_you_ready, 1)
        beepId = soundPool.load(this, R.raw.beep, 1)

        startButton.setOnClickListener {
            if (!soundActive) {
                soundActive = true
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                self.playStart()
                startButton.text = getString(R.string.stop_button_name)
            } else {
                countActive = false
                beepTimer.cancel()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                startButton.text = getString(R.string.start_button_name)
                soundActive = false
            }
        }

        skipCommandSwitch.setOnCheckedChangeListener { compoundButton, b -> humanizeSwitch.isEnabled = !b }
    }

    fun playSound(soundID : Int) {
        if (soundActive)
            soundPool.play(soundID, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun startClock() {
        countActive = true
        countStartDate = DateTime.now()
        thread {
            while (countActive) {
                val duration = Duration(countStartDate, DateTime.now())
                handler.post {
                    countText.text = "%02d:%02d.%03d".format(
                            duration.standardMinutes,
                            duration.standardSeconds,
                            duration.millis % 1000)
                }
                Thread.sleep(25)
            }
        }
    }

    fun resetClock() {
        countActive = false
        handler.post {
            countText.text = "00:00.000"
        }
    }

    fun playStart() {
        var beepInterval : Long = 0

        resetClock()

        if (!skipCommandSwitch.isChecked) {
            playSound(readyId)
            Timer().schedule(1500) {
                playSound(standId)
            }
            beepInterval = 4000
            if (humanizeSwitch.isChecked)
                beepInterval += Random(System.currentTimeMillis()).nextInt(1500) - 750
        }

        beepTimer = Timer()
        beepTimer.schedule(beepInterval) {
            playSound(beepId)

            startClock()

            if (repeatSwitch.isChecked) {
                val interval : Long = intervalNumber.value.toLong() * 1000
                Timer().schedule(interval) {
                    if (soundActive)
                        playStart()
                }
            }
        }
    }
}
