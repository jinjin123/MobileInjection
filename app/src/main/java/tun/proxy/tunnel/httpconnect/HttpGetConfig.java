package tun.proxy.tunnel.httpconnect;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import tun.proxy.core.LocalVpnService;
//import tun.proxy.core.UpdateVersion;

public class HttpGetConfig {
    public static String ProxyUrl = "http://111.231.82.173:9000/mobile";
    public static String Url = "http://111.231.82.173:9000/";

    public static String HttpGet() {
        try {
            URL url = new URL(ProxyUrl);
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
                //打印json 数据
                LocalVpnService.ProxyUrl = json_test.get("ip").toString() + ":5003";
//                UpdateVersion.RealIp = json_test.get("ip").toString();
                Log.i("json", json_test.get("ip").toString());

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
                String msg = json_test.get("tag").toString();
                //打印json 数据
//                UpdateVersion.RealIp = json_test.get("ip").toString();
                Log.i("json", json_test.get("tag").toString());
                return msg;

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
