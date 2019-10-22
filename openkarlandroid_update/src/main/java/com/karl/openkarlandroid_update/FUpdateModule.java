package com.karl.openkarlandroid_update;

import android.content.Context;

public class FUpdateModule {

    private static  String ServerAddress="";

    private static String ApkKey="";

    public static String Tag = "HF";

    private static Context context;

    protected static Context getContext() {
        return context;
    }

    /**
     * 初始化更新模块，日志打印Tag默认为"HF"
     *
     * @param context
     */
    public static void initModule(Context context) {
        initModule(context, "HF");
    }

    public static void initModule(Context context, String Tag) {
        FUpdateModule.context = context;
        FUpdateModule.Tag = Tag;
    }

    public static String getServerAddress() {
        return ServerAddress;
    }

    public static void setServerAddress(String serverAddress) {
        ServerAddress = serverAddress;
    }

    public static String getApkKey() {
        return ApkKey;
    }

    public static void setApkKey(String apkKey) {
        ApkKey = apkKey;
    }
}
