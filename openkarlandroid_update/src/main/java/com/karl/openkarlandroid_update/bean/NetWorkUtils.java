package com.karl.openkarlandroid_update.bean;

import android.text.TextUtils;
import android.util.Log;

import com.karl.openkarlandroid_update.FUpdateModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 网络请求工具类
 */
public class NetWorkUtils {

    /**
     * Http Post请求
     * @param urlStr 请求的地址
     * @param body 请求的发送实体
     * @return
     */
    public static String doHttpPost(String urlStr,String body){
        HttpURLConnection connection=null;
        BufferedReader reader=null;
        try{
            //新建Url
            URL url=new URL(urlStr);
            //发起网络请求
            connection=(HttpURLConnection)url.openConnection();
            //请求方式
            connection.setRequestMethod("POST");
            //设置参数类型是json格式
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            //设置body
            if(!TextUtils.isEmpty(body)) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                writer.write(body);
                writer.close();
            }

            //连接最大时间
            connection.setConnectTimeout(8000);
            //读取最大时间
            connection.setReadTimeout(8000);
            InputStream in=connection.getInputStream();
            //写入reader
            reader=new BufferedReader(new InputStreamReader(in));
            StringBuilder response=new StringBuilder();
            String line;
            while((line=reader.readLine())!=null){
                response.append(line);
            }
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            Log.e(FUpdateModule.Tag,e.getMessage());
        }finally {
            if(reader!=null){
                try{
                    reader.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            if(connection!=null)
                connection.disconnect();
        }
        return  null;
    }



    /**
     * Http Get请求
     * @param urlStr 请求的地址
     * @return
     */
    public static String doHttpGet(String urlStr){
        HttpURLConnection connection=null;
        BufferedReader reader=null;
        try{
            //新建Url
            URL url=new URL(urlStr);
            //发起网络请求
            connection=(HttpURLConnection)url.openConnection();
            //请求方式
            connection.setRequestMethod("GET");
            //设置参数类型是json格式
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");


            //连接最大时间
            connection.setConnectTimeout(8000);
            //读取最大时间
            connection.setReadTimeout(8000);
            InputStream in=connection.getInputStream();
            //写入reader
            reader=new BufferedReader(new InputStreamReader(in));
            StringBuilder response=new StringBuilder();
            String line;
            while((line=reader.readLine())!=null){
                response.append(line);
            }
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            Log.e(FUpdateModule.Tag,e.getMessage());
        }finally {
            if(reader!=null){
                try{
                    reader.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            if(connection!=null)
                connection.disconnect();
        }
        return  null;
    }
}
