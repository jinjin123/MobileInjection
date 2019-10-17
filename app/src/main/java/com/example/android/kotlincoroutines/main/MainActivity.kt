/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.example.android.kotlincoroutines.R
import kotlinx.android.synthetic.main.activity_main.*


/**
 * Main Activity for our application. This activity uses [MainViewModel] to implement MVVM.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG="LocalVPN";
        private const val VPN_REQUEST_CODE = 0x0F
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val rootLayout: Switch = findViewById(R.id.switchButton)
        val viewModel = ViewModelProviders.of(this)
            .get(MainViewModel::class.java)


        switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                intent = VpnService.prepare(this)
                if(intent !=null){
                    startActivityForResult(intent,0)
                }else{
                    onActivityResult(0,RESULT_OK,null)
                }
               viewModel.onMainViewClicked("Connecting....")
            } else {
                Log.d(TAG,"Stop VPN")
                val intent = Intent(this,LocalVpnService::class.java)
                intent.putExtra("COMMAND", "STOP")
                startService(intent)
                viewModel.onMainViewClicked("Close...")

            }
        }

        viewModel.snackbar.observe(this, Observer { text ->
            text?.let {
                Snackbar.make(rootLayout, text, Snackbar.LENGTH_SHORT).show()
                viewModel.onSnackbarShown()
            }

        })
    }

    override fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK){
            val intent = Intent(this, LocalVpnService::class.java)
            startService(intent)
        }
    }
}
