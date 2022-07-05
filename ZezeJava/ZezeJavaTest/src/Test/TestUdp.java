
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class TestUdp {
	public static void main(String args[]) throws IOException {
		var udp = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		var port = udp.getLocalPort();
		System.out.println(port);

		var udp2 = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		var buf = "datagram".getBytes(StandardCharsets.UTF_8);
		udp2.send(new DatagramPacket(buf, buf.length, new InetSocketAddress("127.0.0.1", port)));

		var recv = new byte[1024];
		var p = new DatagramPacket(recv, recv.length);
		udp.receive(p);

		System.out.println(p.getLength());
		var message = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
		System.out.println(message);

		udp.close();
		udp2.close();
	}
}
