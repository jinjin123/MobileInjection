package tun.proxy.core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import tun.proxy.R;
import tun.proxy.tcpip.CommonMethods;
import tun.proxy.tcpip.IPHeader;
import tun.proxy.tcpip.TCPHeader;
import tun.proxy.tcpip.UDPHeader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalVpnService extends VpnService implements Runnable {
    public static LocalVpnService Instance;
    public static String ProxyUrl = "192.168.50.132:8888";
    public static boolean IsRunning = false;

    private static int ID;
    private static int LOCAL_IP;
    private static ConcurrentHashMap<onStatusChangedListener, Object> m_OnStatusChangedListeners = new ConcurrentHashMap<>();

    private Thread m_VPNThread;
    private ParcelFileDescriptor m_VPNInterface;
    private TcpProxyServer m_TcpProxyServer;
    //不需要
    private DnsProxy m_DnsProxy;
    private FileOutputStream m_VPNOutputStream;

    private byte[] m_Packet;
    private IPHeader m_IPHeader;
    private TCPHeader m_TCPHeader;
    private UDPHeader m_UDPHeader;
    private ByteBuffer m_DNSBuffer;
    private Handler m_Handler;
    private long m_SentBytes;
    private long m_ReceivedBytes;

    public LocalVpnService() {
        ID++;
        m_Handler = new Handler();
        m_Packet = new byte[20000];
        m_IPHeader = new IPHeader(m_Packet, 0);
        m_TCPHeader = new TCPHeader(m_Packet, 20);
        m_UDPHeader = new UDPHeader(m_Packet, 20);
        m_DNSBuffer = ((ByteBuffer) ByteBuffer.wrap(m_Packet).position(28)).slice();
        Instance = this;

        Log.i("new vpnservice",String.format("New VPNService(%d)\n", ID));
    }

    @Override
    public void onCreate() {
        IsRunning = true;
        Log.i("VPNService",String.format("VPNService(%s) created.\n", ID));
        // Start a new session by creating a new thread.
        m_VPNThread = new Thread(this, "VPNServiceThread");
        m_VPNThread.start();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if(intent.getStringExtra("COMMAND") == "STOP"){
//            dispose();
//        }
        IsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public interface onStatusChangedListener {
        public void onStatusChanged(String status, Boolean isRunning);

        public void onLogReceived(String logString);
    }

    public static void addOnStatusChangedListener(onStatusChangedListener listener) {
        if (!m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.put(listener, 1);
        }
    }

    public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
        if (m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.remove(listener);
        }
    }


    public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
        try {
            CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
            this.m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, ipHeader.getTotalLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getAppInstallID() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        String appInstallID = preferences.getString("AppInstallID", null);
        if (appInstallID == null || appInstallID.isEmpty()) {
            appInstallID = UUID.randomUUID().toString();
            Editor editor = preferences.edit();
            editor.putString("AppInstallID", appInstallID);
            editor.apply();
        }
        return appInstallID;
    }

    String getVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String version = packInfo.versionName;
            return version;
        } catch (Exception e) {
            return "0.0";
        }
    }

    @Override
    public synchronized void run() {
        try {
            Log.i("VPNService thread",String.format("VPNService(%s) work thread is runing...\n", ID));
            ProxyConfig.AppInstallID = getAppInstallID();//获取安装ID
            ProxyConfig.AppVersion = getVersionName();//获取版本号
            Log.i("AppInstallID ",String.format("AppInstallID: %s", ProxyConfig.AppInstallID));
            Log.i("Android version: ",String.format("Android version: %s", Build.VERSION.RELEASE));
            Log.i("App version: ",String.format("App version: %s", ProxyConfig.AppVersion));


            ChinaIpMaskManager.loadFromFile(getResources().openRawResource(R.raw.ipmask));//加载中国的IP段，用于IP分流。
            waitUntilPreapred();//检查是否准备完毕。

            Log.i("Load config: ",String.format("Load config from file ..."));
            try {
                ProxyConfig.Instance.loadFromFile(getResources().openRawResource(R.raw.config));
                Log.i("Load done: ",String.format("Load done"));
            } catch (Exception e) {
                String errString = e.getMessage();
                if (errString == null || errString.isEmpty()) {
                    errString = e.toString();
                }
                Log.i("Load failed: ",String.format("Load failed with error: %s", errString));
            }

            //本地代理服务
            m_TcpProxyServer = new TcpProxyServer(0);
            m_TcpProxyServer.start();
            Log.i("LocalTcpServer: ",String.format("LocalTcpServer started."));

            m_DnsProxy = new DnsProxy();
            m_DnsProxy.start();
            Log.i("LocalDnsProxy: ",String.format("LocalDnsProxy started."));

            while (true) {
                if (IsRunning) {
                    //加载配置文件
                    Log.i("set proxy: ",String.format("set shadowsocks/(http proxy)"));
                    try {
                        ProxyConfig.Instance.m_ProxyList.clear();
                        ProxyConfig.Instance.addProxyToList(ProxyUrl);
                        Log.i("Proxy is: ",String.format("Proxy is: %s", ProxyConfig.Instance.getDefaultProxy()));
                    } catch (Exception e) {

                        String errString = e.getMessage();
                        if (errString == null || errString.isEmpty()) {
                            errString = e.toString();
                            Log.e("proxy error",errString);
                        }
                        IsRunning = false;
                        continue;
                    }
                    String welcomeInfoString = ProxyConfig.Instance.getWelcomeInfo();
                    if (welcomeInfoString != null && !welcomeInfoString.isEmpty()) {
                        Log.i("welcome : ",String.format("%s", ProxyConfig.Instance.getWelcomeInfo()));
                    }
                    Log.i("mode : ",String.format("Global mode is " + (ProxyConfig.Instance.globalMode ? "on" : "off")));

                    runVPN();
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Log.e("proxy error",e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Fatal : ",String.format("Fatal error: %s", e.toString()));
        } finally {
            Log.i("App terminated : ",String.format("App terminated."));
            dispose();
        }
    }

    /**
     * 开始VPN
     * @throws Exception
     */
    private void runVPN() throws Exception {
        this.m_VPNInterface = establishVPN();
        this.m_VPNOutputStream = new FileOutputStream(m_VPNInterface.getFileDescriptor());
        FileInputStream in = new FileInputStream(m_VPNInterface.getFileDescriptor());
        int size = 0;
        while (size != -1 && IsRunning) {//size=-1表示文件流关闭读不到数据了
            while ((size = in.read(m_Packet)) > 0 && IsRunning) {
                if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped) {
                    in.close();
                    throw new Exception("LocalServer stopped.");
                }
                onIPPacketReceived(m_IPHeader, size);
            }
            Thread.sleep(20);
        }
        in.close();
        disconnectVPN();
    }

    void onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                TCPHeader tcpHeader = m_TCPHeader;
                tcpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP) {
                    if (tcpHeader.getSourcePort() == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
                        NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
                        if (session != null) {
                            ipHeader.setSourceIP(ipHeader.getDestinationIP());
                            tcpHeader.setSourcePort(session.RemotePort);
                            ipHeader.setDestinationIP(LOCAL_IP);

                            CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                            m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                            m_ReceivedBytes += size;
                        } else {
                            Log.i("NoSession: ",String.format("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString()));
                        }
                    } else {
                        // 添加端口映射
                        int portKey = tcpHeader.getSourcePort();
                        NatSession session = NatSessionManager.getSession(portKey);
                        if (session == null || session.RemoteIP != ipHeader.getDestinationIP() || session.RemotePort != tcpHeader.getDestinationPort()) {
                            session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader.getDestinationPort());
                        }

                        session.LastNanoTime = System.nanoTime();
                        session.PacketSent++;//注意顺序

                        int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
                        if (session.PacketSent == 2 && tcpDataSize == 0) {
                            return;//丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
                        }

                        //分析数据，找到host
                        if (session.BytesSent == 0 && tcpDataSize > 10) {
                            int dataOffset = tcpHeader.m_Offset + tcpHeader.getHeaderLength();
                            String host = HttpHostHeaderParser.parseHost(tcpHeader.m_Data, dataOffset, tcpDataSize);
                            if (host != null) {
                                session.RemoteHost = host;
                            } else {
                                Log.i("no host: ",String.format("No host name found: %s", session.RemoteHost));
                            }
                        }

                        // 转发给本地TCP服务器
                        ipHeader.setSourceIP(ipHeader.getDestinationIP());
                        ipHeader.setDestinationIP(LOCAL_IP);
                        tcpHeader.setDestinationPort(m_TcpProxyServer.Port);

                        CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                        m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                        session.BytesSent += tcpDataSize;//注意顺序
                        m_SentBytes += size;
                    }
                }
                break;
//            case IPHeader.UDP:
//                // 转发DNS数据包：
//                UDPHeader udpHeader = m_UDPHeader;
//                udpHeader.m_Offset = ipHeader.getHeaderLength();
//                if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
//                    m_DNSBuffer.clear();
//                    m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
//                    DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
//                    if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
//                        m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
//                    }
//                }
//                break;
        }
    }

    private void waitUntilPreapred() {
        while (prepare(this) != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ParcelFileDescriptor establishVPN() throws Exception {
        Builder builder = new Builder();
        builder.setMtu(ProxyConfig.Instance.getMTU());
        if (ProxyConfig.IS_DEBUG)
            Log.i("setMtu: ",String.format("setMtu: %d\n", ProxyConfig.Instance.getMTU()));

        ProxyConfig.IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
        builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
        if (ProxyConfig.IS_DEBUG)
            Log.i("addAddress: ",String.format("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength));

        for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
            builder.addDnsServer(dns.Address);
            if (ProxyConfig.IS_DEBUG)
                Log.i("addDnsServer: ",String.format("addDnsServer: %s\n", dns.Address));
        }

        if (ProxyConfig.Instance.getRouteList().size() > 0) {
            for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
                builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
                if (ProxyConfig.IS_DEBUG)
                    Log.i("addRoute: ",String.format("addRoute: %s/%d\n", routeAddress.Address, routeAddress.PrefixLength));
            }
            builder.addRoute(CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);

            if (ProxyConfig.IS_DEBUG)
                Log.i("addRoute: ",String.format("addRoute for FAKE_NETWORK: %s/%d\n", CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16));
        } else {
            builder.addRoute("0.0.0.0", 0);
            if (ProxyConfig.IS_DEBUG)
                Log.i("addRoute: ",String.format("addDefaultRoute: 0.0.0.0/0\n"));
        }


        Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
        Method method = SystemProperties.getMethod("get", new Class[]{String.class});
        ArrayList<String> servers = new ArrayList<String>();
        for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
            String value = (String) method.invoke(null, name);
            if (value != null && !"".equals(value) && !servers.contains(value)) {
                servers.add(value);
                if (value.replaceAll("\\d", "").length() == 3) {//防止IPv6地址导致问题
                    builder.addRoute(value, 32);
                } else {
                    builder.addRoute(value, 128);
                }
                if (ProxyConfig.IS_DEBUG)
                    Log.i("addRoute: ",String.format("%s=%s\n", name, value));
            }
        }

        // no idea,this work
        if (AppProxyManager.isLollipopOrAbove) {
                Log.i("proxy : ","Proxy All Apps");
//            if (AppProxyManager.Instance.proxyAppInfo.size() == 0) {
//                Log.i("proxy : ","Proxy All Apps");
//            }
//            for (AppInfo app : AppProxyManager.Instance.proxyAppInfo) {
//                builder.addAllowedApplication("tun.proxy.core");//需要把自己加入代理，不然会无法进行网络连接
//                try {
//                    builder.addAllowedApplication(app.getPkgName());
//                    Log.i("Proxy App: " ,app.getAppLabel());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.i("Proxy App Fail: " , app.getAppLabel());
//                }
//            }
        } else {
            Log.i("No Pre-App proxy", "due to low Android version.");
        }
//        Log.i("proxy : ","Proxy All Apps");
//        builder.addAllowedApplication("tun.proxy.core");//需要把自己加入代理，不然会无法进行网络连接
        builder.setSession(ProxyConfig.Instance.getSessionName());
        ParcelFileDescriptor pfdDescriptor = builder.establish();
        return pfdDescriptor;
    }

    public void disconnectVPN() {
        try {
            if (m_VPNInterface != null) {
                m_VPNInterface.close();
                m_VPNInterface = null;
            }
        } catch (Exception e) {
            // ignore
        }
        this.m_VPNOutputStream = null;
    }

    private synchronized void dispose() {
        // 断开VPN
        disconnectVPN();

        // 停止TcpServer
        if (m_TcpProxyServer != null) {
            m_TcpProxyServer.stop();
            m_TcpProxyServer = null;
            Log.i("LocalTcpServer","stopped.");
        }

        // 停止DNS解析器
        if (m_DnsProxy != null) {
            m_DnsProxy.stop();
            m_DnsProxy = null;
            Log.i("LocalDnsProxy","stopped.");
        }

        stopSelf();
        IsRunning = false;
        System.exit(0);
    }

    @Override
    public void onDestroy() {
        Log.i("VPNService",String.format("VPNService(%s) destoried.\n", ID));
        if (m_VPNThread != null) {
            m_VPNThread.interrupt();
        }
    }

}
