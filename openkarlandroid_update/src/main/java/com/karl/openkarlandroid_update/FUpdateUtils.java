package com.karl.openkarlandroid_update;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karl.openkarlandroid_update.adapter.DateTypeAdapter;
import com.karl.openkarlandroid_update.bean.AppApkVersionInfoResponse;
import com.karl.openkarlandroid_update.bean.AppCommonResponse;
import com.karl.openkarlandroid_update.bean.AppVersionInfo;
import com.karl.openkarlandroid_update.bean.NetWorkUtils;

import java.util.Date;

/**
 * android自动更新的工具
 */
public class FUpdateUtils {
    static String VERSION = "1.0";
    static Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateTypeAdapter()).create();

    public static void TestMethod() {
        /**
         * 获取当前应用的版本信息
         * 执行在子线程中
         */
        AppVersionInfo info = AppVersionUtils.getAppVersionName(FUpdateModule.getContext());
        if (info == null) {
            Log.e(FUpdateModule.Tag, "未获取到版本信息");
            return;
        }
        Log.i(FUpdateModule.Tag, "info.getVersionName" + info.getVersionName() + "  info.getVersioncode" + info.getVersioncode());
        //发送网络请求，获取当前最新版本
        String url = FUpdateModule.getServerAddress();
        String response = NetWorkUtils.doHttpGet(url);
        Log.e(FUpdateModule.Tag, "获取到最新版本" );
        //转换成实体
        try {

            AppCommonResponse appNewestInfo = gson.fromJson(response, AppCommonResponse.class);
            if (VERSION  != BuildConfig.VERSION_NAME){
//                if (appNewestInfo.getData().getVersionCode() > info.getVersioncode()) {
                Log.i(FUpdateModule.Tag, "需要更新");
                //下载
                //启动下载apk文件服务
                Intent intent = new Intent(FUpdateModule.getContext(), DownLoadApkService.class);
                intent.putExtra("data",appNewestInfo.getData());
                FUpdateModule.getContext().startService(intent);
            } else {
                Log.i(FUpdateModule.Tag, "不需更新");
            }
        } catch (Exception ex) {
            Log.e(FUpdateModule.Tag, ex.getMessage());
        }

    }

}
