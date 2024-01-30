package Temp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class TestProcessDaemon {
	public final static String ProcessDaemonPort = "Zeze.ProcessDaemon.Port";
	public final static String ProcessDaemonMMap = "Zeze.ProcessDaemon.MMap";

	public static void main(String[] args) throws Exception {
		var port = System.getProperties().get(ProcessDaemonPort);
		System.out.println("Start...");
		if (null != port) {
			System.out.println("subProcess peer=" + port);
			subProcess(Integer.parseInt((String)port));
		} else {
			System.out.println("DaemonProcess daemon");
			daemonProcess();
		}
	}

	private static void subProcess(int peer) throws Exception {
		var udp = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		var port = udp.getLocalPort();
		var sendBuf = String.valueOf(port).getBytes(StandardCharsets.UTF_8);
		var p = new DatagramPacket(sendBuf, sendBuf.length, new InetSocketAddress("127.0.0.1", peer));
		udp.send(p);
		var recvBuf = new byte[1024];
		p = new DatagramPacket(recvBuf, recvBuf.length);
		udp.receive(p);
		var message = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
		System.out.println(message);
		System.out.println("subprocess exit");
		//Thread.sleep(2000);
	}

	private static void daemonProcess() throws IOException, InterruptedException {
		var udp = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		var port = udp.getLocalPort();
		System.out.println("exec begin");
		var pb = new ProcessBuilder(
				"java",
				"-cp",
				"C:/Users/10501/Desktop/code/zeze/ZezeJava/ZezeJavaTest/build/classes/java/main",
				"-D" + ProcessDaemonPort + "=" + port,
				"-D" + ProcessDaemonMMap + "=",
				"Temp.TestProcessDaemon"
		);
		var sub = pb.inheritIO().start();
		System.out.println("exec end");
		var buf = new byte[1024];
		var p = new DatagramPacket(buf, buf.length);
		udp.receive(p);
		var message = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
		var peer = Integer.parseInt(message);
		var hello = "hello".getBytes(StandardCharsets.UTF_8);
		p = new DatagramPacket(hello, hello.length, new InetSocketAddress("127.0.0.1", peer));
		udp.send(p);
		//sub.destroy();
		System.out.println("waitFor=" + sub.waitFor());
		System.out.println("daemonProcess exit");
	}
}
