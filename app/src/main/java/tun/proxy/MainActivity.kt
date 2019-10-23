package tun.proxy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import tun.proxy.core.LocalVpnService
import tun.proxy.core.ProxyConfig
import com.karl.openkarlandroid_update.FUpdateModule
import com.karl.openkarlandroid_update.FUpdateUtils
import tun.util.PermissionsUtil
import android.text.TextUtils
import com.tbruyelle.rxpermissions2.RxPermissions



/**
 * Main Activity for our application. This activity uses [MainViewModel] to implement MVVM.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG="LocalVPN";

    }

    private val PERMISSION_REQUEST_CODE = 64
    private var isRequireCheck: Boolean = false

    private var permission: Array<String> = emptyArray()
    private var key: String? = null
    private var showTip: Boolean = false
    private var tipInfo: PermissionsUtil.TipInfo? = null

    private val defaultTitle = "帮助"
    private val defaultContent = "当前应用缺少必要权限。\n \n 请点击 \"设置\"-\"权限\"-打开所需权限。"
    private val defaultCancel = "取消"
    private val defaultEnsure = "设置"

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

    private fun checkPermission() {
        isRequireCheck = true
//        permission = intent.getStringArrayExtra("permission")
//        key = intent.getStringExtra("key")
//        showTip = intent.getBooleanExtra("showTip", true)
//        val ser = intent.getSerializableExtra("tip")
//        if (ser == null) {
//            tipInfo = PermissionsUtil.TipInfo(defaultTitle, defaultContent, defaultCancel, defaultEnsure)
//        } else {
//            tipInfo = ser as PermissionsUtil.TipInfo
//        }

//        PermissionsUtil.getInstance().requestPermission(this.applicationContext, object : PermissionListener {
//            override fun permissionGranted(permissions: Array<String>) {
//                //用户同意
//                Log.e(TAG,"yes")
//            }
//
//            override fun permissionDenied(permissions: Array<String>) {
//                //用户拒绝了访问的申请
//                Log.e(TAG,"no")
//            }
//        }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)

        val d = RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        getLatestApk()
//                        if (PermissionsUtil.getInstance().hasPermission(this@MainActivity, this.permission)) {
//                            permissionsGranted()
//                        } else {
//                            permission?.let { requestPermissions(it) } // 请求权限,回调时会触发onResume
//                            isRequireCheck = false
//                        }
                    } else {
//                        isRequireCheck = true
                        showMissingPermissionDialog()
                    }
                }
    }
    private fun getLatestApk() {
        FUpdateModule.initModule(this)
        FUpdateModule.setServerAddress("http://111.231.82.173/file/app-release.apk")
        FUpdateModule.setApkKey("85068642-fa90-4ce8-bfa9-67a284914807")
        object : Thread() {
            override fun run() {
                FUpdateUtils.TestMethod()
            }
        }.start()
    }
//    override fun onResume() {
//        super.onResume()
//        if (isRequireCheck) {
//            if (PermissionsUtil.getInstance().hasPermission(this@MainActivity, permission)) {
//                permissionsGranted()
//            } else {
//                permission?.let { requestPermissions(it) } // 请求权限,回调时会触发onResume
//                isRequireCheck = false
//            }
//        } else {
//            isRequireCheck = true
//        }
//    }

    // 请求权限兼容低版本
//    private fun requestPermissions(permission: Array<String>) {
//        ActivityCompat.requestPermissions(this, permission, PERMISSION_REQUEST_CODE)
//    }
    /**
     * 用户权限处理,
     * 如果全部获取, 则直接过.
     * 如果权限缺失, 则提示Dialog.
     *
     * @param requestCode  请求码
     * @param permissions  权限
     * @param grantResults 结果
     */
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//
//        //部分厂商手机系统返回授权成功时，厂商可以拒绝权限，所以要用PermissionChecker二次判断
//        if (requestCode == PERMISSION_REQUEST_CODE && PermissionsUtil.getInstance().isGranted(*grantResults)
//                && PermissionsUtil.getInstance().hasPermission(this, permissions)) {
//            permissionsGranted()
//        } else if (showTip) {
//            showMissingPermissionDialog()
//        } else { //不需要提示用户
//            permissionsDenied()
//        }
//    }


    // 显示缺失权限提示
    private fun showMissingPermissionDialog() {

        val builder = AlertDialog.Builder(this)

        builder.setTitle(if (TextUtils.isEmpty(tipInfo?.title)) defaultTitle else tipInfo?.title)
        builder.setMessage(if (TextUtils.isEmpty(tipInfo?.content)) defaultContent else tipInfo?.content)

        // if quick click will open the vpn switch
//        builder.setNegativeButton(if (TextUtils.isEmpty(tipInfo?.cancel)) defaultCancel else tipInfo?.cancel) { dialog, which ->
//            permissionsDenied()
//            dialog.dismiss()
//        }

        builder.setPositiveButton(if (TextUtils.isEmpty(tipInfo?.ensure)) defaultEnsure else tipInfo?.ensure) { dialog, which ->
            PermissionsUtil.getInstance().gotoSetting(this)
            dialog.dismiss()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun permissionsDenied() {
        val listener = PermissionsUtil.getInstance().fetchListener(key)
        listener?.permissionDenied(permission)
        showMissingPermissionDialog()
    }
//
//    // 全部权限均已获取
//    private fun permissionsGranted() {
//        val listener = PermissionsUtil.getInstance().fetchListener(key)
//        listener?.permissionGranted(permission)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        PermissionsUtil.getInstance().fetchListener(key)
//        if (tipInfo != null) {
//            tipInfo = null
//        }
//    }

    override fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK){
            val intent = Intent(this, LocalVpnService::class.java)
            startService(intent)
        }
    }
}
