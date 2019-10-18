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

package tun.proxy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tun.util.BACKGROUND


class MainViewModel : ViewModel() {


    private val _snackBar = MutableLiveData<String>()

    val snackbar: LiveData<String>
        get() = _snackBar

    fun onMainViewClicked(msg: String) {
        // TODO: Replace with coroutine implementation
        BACKGROUND.submit {
            Thread.sleep(1_000)
            // use postValue since we're in a background thread
            _snackBar.postValue(msg)
        }
    }


    /**
     * Called immediately after the UI shows the snackbar.
     */
    fun onSnackbarShown() {
        _snackBar.value = null
    }
}
