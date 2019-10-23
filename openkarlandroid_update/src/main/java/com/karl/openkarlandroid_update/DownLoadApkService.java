package com.karl.openkarlandroid_update;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.karl.openkarlandroid_update.bean.AppApkVersionInfoResponse;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * 下载最新Apk文件的Service
 * 请以startService方式启动
 */
public class DownLoadApkService extends Service {

    DownloadManager downloadManager;
    private long mTaskId;

    public DownLoadApkService() {

    }

    //广播接受者，接收下载状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkDownloadStatus();//检查下载状态
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        deleteapk(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "freedom.apk"));
        //获取文件下载的地址
//        AppApkVersionInfoResponse response = (AppApkVersionInfoResponse) intent.getSerializableExtra("data");

        //创建下载任务
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(FUpdateModule.getServerAddress()));
        request.setAllowedOverRoaming(false);//漫游网络是否可以下载

        //设置文件类型，可以在下载结束后自动打开该文件
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(FUpdateModule.getServerAddress() ));
        request.setMimeType(mimeString);

        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(true);

        //sdcard的目录下的download文件夹，必须设置
//        String saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + response.getApkInfoName() + "V" + response.getVersionId();
        request.setDestinationInExternalPublicDir("/download/", "freedom" + ".apk");
        //request.setDestinationInExternalFilesDir(),也可以自己制定下载路径

        //将下载请求加入下载队列
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //加入下载队列后会给该任务返回一个long型的id，
        //通过该id可以取消任务，重启任务等等，看上面源码中框起来的方法
        mTaskId = downloadManager.enqueue(request);

        //注册广播接收者，监听下载状态
        registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        Log.i(FUpdateModule.Tag, "开始下载");
//        checkDownloadStatus();
        return START_NOT_STICKY;
    }


    //检查下载状态
    private void checkDownloadStatus()   {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mTaskId);//筛选下载任务，传入任务ID，可变参数
//        boolean install = true;
//        while(install){
            Cursor c = downloadManager.query(query);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                        Log.i(FUpdateModule.Tag, ">>>下载暂停");
                    case DownloadManager.STATUS_PENDING:
                        Log.i(FUpdateModule.Tag, ">>>下载延迟");
                    case DownloadManager.STATUS_RUNNING:
                        Log.i(FUpdateModule.Tag, ">>>正在下载");
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.i(FUpdateModule.Tag, ">>>下载完成");
                        //下载完成安装APK
//                    downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "freedom.apk";
                        installApp(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "freedom.apk"));
//                        openAPK(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "freedom.apk");
//                        install = false;
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Log.i(FUpdateModule.Tag, ">>>下载失败");
                        break;
                }

            }
//        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void deleteapk(File file){
        if(file.exists()){
            file.delete();
        }
    }
    /**
     * 安装apk
     * @param
     */
    private void installApp(File file) {
        if (file == null || !file.getPath().endsWith(".apk")) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);


        //判读版本是否在7.0以上
        if (Build.VERSION.SDK_INT >= 24) {
            Uri apkUri = FileProvider.getUriForFile(this, "tun.proxy.fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }
//    private void openAPK(String fileSavePath){
//        File file=new File(Uri.parse(fileSavePath).getPath());
//        String filePath = file.getAbsolutePath();
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri data = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//判断版本大于等于7.0
//            data = FileProvider.getUriForFile(this, "tun.proxy", new File(filePath));
//            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);// 给目标应用一个临时授权
//        } else {
//            data = Uri.fromFile(file);
//        }
//
//        intent.setDataAndType(data, "application/vnd.android.package-archive");
//        startActivity(intent);
//    }
}
