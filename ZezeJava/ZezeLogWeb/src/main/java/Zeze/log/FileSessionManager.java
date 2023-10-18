package Zeze.log;

import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileSessionManager {

    public static Map<String, Object> map = new LinkedHashMap<>(1000);

    public synchronized static void put(SocketAddress socketAddress, Object session){
        String ip = getIP(socketAddress);
        map.put(ip, session);
    }


    public static Object get(SocketAddress socketAddress){
        String ip = getIP(socketAddress);
        Object o = map.get(ip);
        return o;
    }

    private static String getIP(SocketAddress socketAddress){
        String addr = socketAddress.toString();
        return addr.split(":")[0];
    }


}
