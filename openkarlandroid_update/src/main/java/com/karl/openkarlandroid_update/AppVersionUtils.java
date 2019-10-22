package com.karl.openkarlandroid_update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.karl.openkarlandroid_update.bean.AppVersionInfo;

/**
 * 当前应用信息获取的帮助类
 *
 */
public class AppVersionUtils {

    /**
     * 返回当前程序版本名
     * @param context
     * @return 返回当前程序的版本名以及版本号问题，若获取失败，则返回null
     */
    public static AppVersionInfo getAppVersionName(Context context) {
        AppVersionInfo result=new AppVersionInfo();
        String versionName = "";
        int versioncode=0;
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            versioncode = pi.versionCode;
            if (versionName == null || versionName.length() <= 0) {
                return null;
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        result.setVersionName(versionName);
        result.setVersioncode(versioncode);
        return result;
    }
}
