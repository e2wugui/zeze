package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class TestNioRebind {
	public static void main(String[] args) throws Exception {
		var ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ServerSocket ss = ssc.socket();
		ss.setReuseAddress(true);
		ss.bind(new InetSocketAddress(6004), 50);

		var selector = Selector.open();
		SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("bind");

//		selector.select(key -> {
//			try {
//				((ServerSocketChannel) key.channel()).accept();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		});
//		System.out.println("selected");

		// Thread.sleep(1000);
		System.out.println("close");

		// ssc.register(selector, 0);
		// key.cancel();
		ssc.close();
		// ss.close();
		// selector.close(); // 只有加这一行才能再次bind

		selector.selectNow(); // 这一行也能让刚刚close的server socket channel彻底关闭socket

		// Thread.sleep(1000);
		System.out.println("rebind");

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ss = ssc.socket();
		ss.setReuseAddress(true);
		ss.bind(new InetSocketAddress(6004), 50);

		// Thread.sleep(1000);
		System.out.println("OK");
	}
}
