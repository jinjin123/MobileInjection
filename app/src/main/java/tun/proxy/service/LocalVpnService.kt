package tun.proxy.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer

import java.nio.channels.SocketChannel

import java.io.DataInputStream


//import org.pcap4j.packet.IpV4Packet
//import org.pcap4j.packet.namednumber.IpNumber

class LocalVpnService : VpnService(){
    companion object {
        private const val TAG = "LocalVpnService"
    }
    init {
        System.loadLibrary("tun2http")
    }
    private var mThread : Thread? = null
    private var  vpn : ParcelFileDescriptor? = null
    private var  mInterface : ParcelFileDescriptor? = null
    var builder = Builder()

    private external fun jni_init()
    private external fun jni_start(tun: Int?, fwd53: Boolean, rcode: Int, proxyIp: String, proxyPort: Int)

    private external fun jni_stop(tun: Int)

    private external fun jni_get_mtu(): Int
    private external fun jni_done()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Received $intent")
        if (intent == null) {
            return START_STICKY
        }
        if(intent?.getStringExtra("COMMAND") == "STOP"){
            stopVpn()
        }

        return START_STICKY
    }

    override fun onCreate() {
        jni_init()
        super.onCreate()
        setupVpn()
        startvpn()
    }
    private  fun setupVpn() {
        //must has intenet
        var mtu: Int = jni_get_mtu()
        Log.i(TAG, "MTU=" + mtu);
        var builder = Builder()
                .addAddress("10.0.1.1", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setSession(TAG)
                .setMtu(mtu)
        mInterface = builder.establish()
        if (mInterface == null){
            Log.e(TAG,"tunnel open faild")
        }
//        jni_start(mInterface?.fd,false,3,"192.168.50.132",3213)
        Log.d(TAG, "VPN interface has established")
    }


    fun startvpn() {
        Thread(Runnable {
            run {
                val inputStream = FileInputStream(mInterface?.getFileDescriptor())
                val outputStream = FileOutputStream(mInterface?.getFileDescriptor())
//                    val inputStream = FileInputStream(mInterface!!.fileDescriptor).channel
//                    val outputStream = FileOutputStream(mInterface!!.fileDescriptor).channel
//                    var tunnel = DatagramChannel.open()
//                    tunnel.connect(InetSocketAddress("192.168.50.132",8888))
//                    Log.i("Localhost: ",InetAddress.getLocalHost().toString())
                val buffer = ByteBuffer.allocate(1024)
//                    protect(tunnel.socket())
                var ok = true
                while (ok) {
                    val tcpSocket = SocketChannel.open().socket()
                    try {
                        val readBytes = inputStream.read(buffer.array())
                        if (readBytes > 0) {
                            Log.i(TAG, "DATA:${readBytes.toString()}")
                            buffer.limit(readBytes)
                            tcpSocket.connect(InetSocketAddress("192.168.50.132", 8888))
//                            val socketadd = InetSocketAddress("192.168.50.132", 8888)
//                            var packet = DatagramPacket(buffer.array(), 0, readBytes)
//                            val packet = IpV4Packet.newPacket(buffer.array(), 6, readBytes)
//                            Log.d(TAG, "REQUEST\n${packet}")
//                            Log.d(TAG, "REQUEST\n${packet.address}")
                            protect(tcpSocket)
                            val outBuffer = tcpSocket.getOutputStream()
                            outBuffer.write(buffer.array())
                            outBuffer.flush()
                            buffer.clear()
                        }
                        if (tcpSocket.isConnected) {
                            val inBuffer = tcpSocket.getInputStream()
                            val inStream = DataInputStream(inBuffer)
                            Log.i(TAG, "Response length " + inStream.available())
                            if (inStream.available() > 0) {
                                Log.i(TAG, "Server says " + inStream.readUTF())
                                inStream.readFully(buffer.array())
                                outputStream.write(buffer.array())
                                inBuffer.close()
                            }
                            outputStream.flush()
                        }
                        buffer.clear()
//                        val remoteBytes = tunnel.read(buffer)
//                        if (remoteBytes > 0) {
//                            Log.d(TAG, "REPONSE\n${remoteBytes}")
//                            buffer.limit(remoteBytes)
//                            outputStream.write(arrayOf(buffer), 0, remoteBytes)
//                            buffer.clear()
//
//                        } else {
//                            Log.d(TAG, "REPONSE\n${remoteBytes}")
//                        }
//                        Thread.sleep(100)
                    } catch (e: Exception) {
                        e.printStackTrace();
                        Log.e(TAG, e.toString())
                        ok = false
                    }
                    tcpSocket.close()
                }
                inputStream.close()
                outputStream.close()
            }
        }).start()
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroy")
//        jni_done()
        super.onDestroy()
        stopSelf()
    }
    private fun stopVpn(){
        mInterface?.close()
        stopSelf()
        Log.i(TAG, "Stopped VPN")
    }
}