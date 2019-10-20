package tun.proxy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import tun.proxy.core.LocalVpnService
import tun.proxy.core.ProxyConfig
import com.weimu.universalview.core.BaseB
import com.weimu.universalview.helper.RxSchedulers
import com.pmm.silentupdate.SilentUpdate
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
        checkPermission()
        val rootLayout: Switch = findViewById(R.id.switchButton)
        val viewModel = ViewModelProviders.of(this)
            .get(MainViewModel::class.java)


        switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ProxyConfig.Instance.globalMode = true
                intent = VpnService.prepare(this)
                if(intent !=null){
                    startActivityForResult(intent,0)
                }else{
                    onActivityResult(0,RESULT_OK,null)
                }
               viewModel.onMainViewClicked("Connecting....")
            } else {
//                ProxyConfig.Instance.globalMode = false
                LocalVpnService.IsRunning = false
                Log.d(TAG,"Stop VPN")
//                val intent = Intent(this, LocalVpnService::class.java)
//                intent.putExtra("COMMAND", "STOP")
//                startService(intent)
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
    class CheckVersionResultPO(
            val apkUrl: String,
            val latestVersion: String
    ) : BaseB()
    private fun checkPermission() {
        val d = RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) getLatestApk()
                }
    }
    private fun getLatestApk() {
        //具体的网络请求步骤自己操作
        val d = Observable.just(CheckVersionResultPO(
                apkUrl = "http://111.231.82.173/file/app-release.apk",
                latestVersion = "1.1.1"
        )).compose(RxSchedulers.toMain())
                .subscribe {
                    //判断版本号
                    if (it.latestVersion > BuildConfig.VERSION_NAME) {
                        Toast.makeText(this@MainActivity, "开始下载中...", Toast.LENGTH_SHORT).show()

                        SilentUpdate.update {
                            this.apkUrl = it.apkUrl
                            this.latestVersion = it.latestVersion
                            this.msg = "1.bug修复"
                            this.isForce = true
                            this.extra = Bundle()
                        }
                    }
                }
    }

    override fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK){
            val intent = Intent(this, LocalVpnService::class.java)
            startService(intent)
        }
    }
}
