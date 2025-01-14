package Zeze.Net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutInt;

/**
 * 解析haproxy header，把信息保存下来。
 * 启用需要在Service里面配置。
 * Service会构造这个类并设置到AsyncSocket里面。
 * https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt
 */
public class HaProxyHeader {
	private boolean done = false;
	private final String key;
	private InetSocketAddress remoteAddress;
	private InetSocketAddress targetAddress;

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

	public static String findV1Line(byte[] bytes, int offset, int end, OutInt endOffset) {
		endOffset.value = offset;
		for (; endOffset.value < end; ++endOffset.value) {
			if (bytes[endOffset.value] == 0x0D) {
				if (endOffset.value < end - 1) {
					endOffset.value += 1;
					if (bytes[endOffset.value] == 0x0A)
						return new String(bytes, offset, endOffset.value - offset - 1, StandardCharsets.UTF_8);
					throw new RuntimeException("haproxy error line end.");
				}
				break;
			}
		}
		return null;
	}

	public boolean decodeHeader(ByteBuffer bb) throws UnknownHostException {
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
					// port读出来，再拼成InetSocketAddress吧。当然拼成Inet，就不需要单独保存了。
					var remoteInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 20));
					remoteAddress = new InetSocketAddress(remoteInet4Address, javaBb.getShort(bb.ReadIndex + 24));

					var targetInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 20, bb.ReadIndex + 24));
					targetAddress = new InetSocketAddress(targetInet4Address, javaBb.getShort(bb.ReadIndex + 26));

					bb.ReadIndex += size;
					done = true;
					return true;

				case 0x21: /* TCPv6 */
					var remoteInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 32));
					remoteAddress = new InetSocketAddress(remoteInet6Address, javaBb.getShort(bb.ReadIndex + 48));

					var targetInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 32, bb.ReadIndex + 48));
					targetAddress = new InetSocketAddress(targetInet6Address, javaBb.getShort(bb.ReadIndex + 50));;

					bb.ReadIndex += size;
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
			var endOffset = new OutInt();
			var line = findV1Line(bb.Bytes, bb.ReadIndex + v1sig.length, bb.WriteIndex, endOffset);
			if (null == line) {
				if (bb.size() > 108)
					throw new RuntimeException("haproxy v1 line too long.");
				return false;
			}
			if (!line.equals("UNKNOWN")) {
				/* parse the V1 header using favorite address parsers like inet_pton. */
				var tokens = line.split(" ");
				if (tokens.length >= 5) {
					switch (tokens[0]) {
					case "TCP4", "TCP6": // 两个协议都用InetAddress.getByName，实现内部会区分。
						remoteAddress = new InetSocketAddress(InetAddress.getByName(tokens[1]), Integer.parseInt(tokens[3]));
						targetAddress = new InetSocketAddress(InetAddress.getByName(tokens[2]), Integer.parseInt(tokens[4]));
						break;
					}
				}
			}
			bb.ReadIndex += endOffset.value + 1; // endOffset 指向最后的ox0A，需要去掉。
			done = true;
			return true;
		}
		else
			throw new RuntimeException("haproxy wrong protocol.");

		return done;
	}

	public InetSocketAddress getTargetAddress() {
		return targetAddress;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}
}
