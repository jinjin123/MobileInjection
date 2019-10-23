package com.karl.openkarlandroid_update;

import android.content.Context;
import android.content.Intent;
import android.util.LayoutDirection;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karl.openkarlandroid_update.adapter.DateTypeAdapter;
import com.karl.openkarlandroid_update.bean.AppApkVersionInfoResponse;
import com.karl.openkarlandroid_update.bean.AppCommonResponse;
import com.karl.openkarlandroid_update.bean.AppVersionInfo;
import com.karl.openkarlandroid_update.bean.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * android自动更新的工具
 */
public class FUpdateUtils {
    static String VERSION = "1";
    static Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateTypeAdapter()).create();
    public static String Url = "http://111.231.82.173:9000/";
    public static String lastVersion;

    public static void TestMethod() {
        HttpGetExt("mversion");
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
        try {
            if (lastVersion.trim().equals("")){
                Log.e(FUpdateModule.Tag, lastVersion);
//                String url = FUpdateModule.getServerAddress();
//                String response = NetWorkUtils.doHttpGet(url);
//                AppCommonResponse appNewestInfo = gson.fromJson(response, AppCommonResponse.class);
                Log.e(FUpdateModule.Tag, "获取到最新版本" );
                //下载
                //启动下载apk文件服务
                Intent intent = new Intent(FUpdateModule.getContext(), DownLoadApkService.class);
//                intent.putExtra("data",appNewestInfo.getData());
                FUpdateModule.getContext().startService(intent);
            } else {
                Log.i(FUpdateModule.Tag, "不需更新");
            }
        } catch (Exception ex) {
            Log.e(FUpdateModule.Tag, ex.getMessage());
        }

    }

    public static String HttpGetExt(String Tag) {
        try {
            URL url = new URL(Url + Tag);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            InputStream inputStream = null;
            BufferedReader reader = null;
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //获得连接的输入流
                inputStream = conn.getInputStream();
                //转换成一个加强型的buffered流
                reader = new BufferedReader(new InputStreamReader(inputStream));
                //把读到的内容赋值给result
                String result = reader.readLine();
                JSONObject json_test = new JSONObject(result);
                lastVersion =  json_test.get("tag").toString();
                //打印json 数据
//                UpdateVersion.RealIp = json_test.get("ip").toString();
                Log.e("json", json_test.get("tag").toString());

            }
            //关闭流和连接
            reader.close();
            inputStream.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

}
