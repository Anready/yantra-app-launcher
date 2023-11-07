package com.coderGtm.yantra.activities

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.android.billingclient.api.BillingClient
import com.coderGtm.yantra.R
import com.coderGtm.yantra.SHARED_PREFS_FILE_NAME
import com.coderGtm.yantra.YantraLauncher
import com.coderGtm.yantra.databinding.ActivityMainBinding
import com.coderGtm.yantra.getAppsList
import com.coderGtm.yantra.requestCmdInputFocusAndShowKeyboard
import com.coderGtm.yantra.requestUpdateIfAvailable
import com.coderGtm.yantra.terminal.Terminal
import com.coderGtm.yantra.views.TerminalGestureListenerCallback


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, TerminalGestureListenerCallback {

    private lateinit var primaryTerminal: Terminal
    private lateinit var app: YantraLauncher
    private lateinit var binding: ActivityMainBinding
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as YantraLauncher
        app.preferenceObject = applicationContext.getSharedPreferences(SHARED_PREFS_FILE_NAME,0)

        primaryTerminal = Terminal(
            activity = this@MainActivity,
            binding = binding,
            preferenceObject = app.preferenceObject
        )
        primaryTerminal.initialize()
    }

    override fun onStart() {
        super.onStart()
        Thread {
            primaryTerminal.appList = getAppsList(primaryTerminal)
            val initList = getInit()
            runInitTasks(initList)
        }.start()
    }

    override fun onSingleTap() {
        val oneTapKeyboardActivation = app.preferenceObject.getBoolean("oneTapKeyboardActivation",true)
        if (oneTapKeyboardActivation) {
            requestCmdInputFocusAndShowKeyboard(this@MainActivity, binding)
        }
    }

    override fun onDoubleTap() {
        val cmdToExecute = app.preferenceObject.getString("doubleTapCommand", "lock")
        if (cmdToExecute != "") {
            //execute command
            primaryTerminal.handleCommand(cmdToExecute!!)
        }
    }

    override fun onInit(p0: Int) {
        TODO("Not yet implemented")
    }

    private fun getInit(): String {
        return try {
            app.preferenceObject.getString("initList", "") ?: ""
        } catch (e: ClassCastException) {
            // prev Set implementation present
            app.preferenceObject.edit().remove("initList").apply()
            ""
        }
    }
    private fun runInitTasks(initList: String?) {
        if (initList?.trim() != "") {
            val initCmdLog = app.preferenceObject.getBoolean("initCmdLog", false)
            runOnUiThread {
                initList?.lines()?.forEach {
                    primaryTerminal.handleCommand(it.trim(), logCmd = initCmdLog)
                }
            }
        }
    }
    override fun onRestart() {
        super.onRestart()
        val unwrappedCursorDrawable = AppCompatResources.getDrawable(this,
            R.drawable.cursor_drawable
        )
        val wrappedCursorDrawable = DrawableCompat.wrap(unwrappedCursorDrawable!!)
        DrawableCompat.setTint(wrappedCursorDrawable, primaryTerminal.theme.buttonColor)
        Thread {
            requestUpdateIfAvailable(app.preferenceObject, this@MainActivity)
        }.start()
    }
    override fun onResume() {
        super.onResume()
        if (primaryTerminal.uninstallCmdActive) {
            primaryTerminal.uninstallCmdActive = false
            primaryTerminal.appList = getAppsList(primaryTerminal)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            billingClient.endConnection()
        }
        catch(_: java.lang.Exception) {}
    }
}
