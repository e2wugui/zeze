package Zeze.Net;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import Zeze.Serialize.ByteBuffer;

/**
 * 解析haproxy header，把信息保存下来。
 * 启用需要在Service里面配置。
 * Service会构造这个类并设置到AsyncSocket里面。
 * https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt
 */
public class HaProxyHeader {
	private boolean done = false;
	private final String key;
	private SocketAddress remoteAddress;
	private int remotePort;
	private SocketAddress targetAddress;
	private int targetPort;

	public HaProxyHeader(String key) {
		this.key = key;
	}

	public static final byte[] v2sig = { 0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A };
	public static final byte[] v1sig = "PROXY ".getBytes(StandardCharsets.UTF_8);

	/**
	 * bb 必须有足够的数据
	 * @param bb 数据buffer
	 * @return true start with v2sig
	 */
	public static boolean startWithV2sig(byte[] bb, int offset) {
		for (int i = 0; i < v2sig.length; ++i) {
			if (bb[offset + i] != v2sig[i])
				return false;
		}
		return true;
	}

	public static boolean startWithV1sig(byte[] bb, int offset) {
		for (int i = 0; i < v1sig.length; ++i) {
			if (bb[offset + i] != v1sig[i])
				return false;
		}
		return true;
	}

	public static String findV1Line(byte[] bytes, int offset, int end) {
		var i = offset;
		for (; i < end; ++i) {
			if (bytes[i] == 0x0D) {
				if (i < end - 1) {
					if (bytes[i + 1] == 0x0A)
						return new String(bytes, offset, i - offset - 1, StandardCharsets.UTF_8);
					throw new RuntimeException("haproxy error line end.");
				}
				break;
			}
		}
		return null;
	}

	public boolean decodeHeader(ByteBuffer bb) {
		if (done)
			return true;

		// 16 = v2gig(12) + version_command(1) + family(1) + length(2)
		if (bb.size() >= 16 && startWithV2sig(bb.Bytes, bb.ReadIndex) && ((bb.Bytes[bb.ReadIndex + v2sig.length] & 0xF0) == 0x20)) {
			var javaBb = java.nio.ByteBuffer.wrap(bb.Bytes);
			var size = 16 + javaBb.getShort(14); // offset 14 是个 short，需要 ntohs。
			if (bb.size() < size)
				return false; // not enough data
			var cmd = bb.Bytes[bb.ReadIndex + v2sig.length] & 0xF;
			var fam = bb.Bytes[bb.ReadIndex + v2sig.length + 1];
			switch (cmd) {
			case 0x01: /* proxy command */
				switch (fam) {
				case 0x11: /* TCPv4 */
					//remoteAddress = new InetSocketAddress(InetAddress.getByAddress(), );
					/*
					remoteAddress = bb.ReadIndex + 16;
					targetAddress = bb.ReadIndex + 20;
					*/
					// port读出来，再拼成InetSocketAddress吧。当然拼成Inet，就不需要单独保存了。
					remotePort = javaBb.getShort(bb.ReadIndex + 24);
					targetPort = javaBb.getShort(bb.ReadIndex + 26);
					done = true;
					return true;

				case 0x21: /* TCPv6 */
					/*
					remoteAddress = bb.ReadIndex + 16;
					targetAddress = bb.ReadIndex + 32;
					*/
					remotePort = javaBb.getShort(bb.ReadIndex + 48);
					targetPort = javaBb.getShort(bb.ReadIndex + 50);
					done = true;
					return true;
				}
				break;

			case 0x00: /* LOCAL command */
				/* keep local connection address for LOCAL */
				break;

			default:
				throw new RuntimeException("haproxy not a supported command");
			}
		}
		// PROXY ...\r\n
		else if (bb.size() >= 8 && startWithV1sig(bb.Bytes, bb.ReadIndex)) {
			var line = findV1Line(bb.Bytes, bb.ReadIndex + v1sig.length, bb.WriteIndex);
			if (null == line) {
				if (bb.size() > 108)
					throw new RuntimeException("haproxy v1 line too long.");
				return false;
			}
			if (!line.equals("UNKNOWN")) {
				/* parse the V1 header using favorite address parsers like inet_pton. */
				var tokens = line.split(" ");
				// todo decode line;
			}
		}
		else
			throw new RuntimeException("haproxy wrong protocol.");

		return done;
	}

	public SocketAddress getTargetAddress() {
		return targetAddress;
	}

	public int getTargetPort() {
		return targetPort;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public int getRemotePort() {
		return remotePort;
	}
}
