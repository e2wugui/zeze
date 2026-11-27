package Zeze.Net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 解析haproxy header，把信息保存下来。
 * 启用需要在Service里面配置。
 * Service会构造这个类并设置到AsyncSocket里面。
 * <a href="https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt">proxy-protocol.txt</a>
 */
public class HaProxyHeader {
	private static final Logger logger = LogManager.getLogger(HaProxyHeader.class);

	private boolean done = false;
	private final @Nullable String key;
	private volatile @Nullable InetSocketAddress remoteAddress;
	private volatile @Nullable InetSocketAddress targetAddress;
	// v1 地址推迟到 getter 懒解析，避免在 selector 线程触发同步 DNS 阻塞。
	private volatile @Nullable String remoteHost;
	private volatile @Nullable String targetHost;
	private int remotePort;
	private int targetPort;

	public HaProxyHeader(@Nullable String key) {
		this.key = key;
	}

	public @Nullable String getKey() {
		return key;
	}

	public @Nullable InetSocketAddress getRemoteAddress() {
		var addr = remoteAddress;
		if (addr != null)
			return addr;
		var host = remoteHost;
		if (host == null)
			return null;
		return remoteAddress = resolve(host, remotePort);
	}

	public @Nullable InetSocketAddress getTargetAddress() {
		var addr = targetAddress;
		if (addr != null)
			return addr;
		var host = targetHost;
		if (host == null)
			return null;
		return targetAddress = resolve(host, targetPort);
	}

	private static @Nullable InetSocketAddress resolve(String host, int port) {
		try {
			return new InetSocketAddress(InetAddress.getByName(host), port);
		} catch (UnknownHostException | IllegalArgumentException e) {
			logger.warn("HaProxyHeader resolve failed: {}:{}", host, port, e);
			return null;
		}
	}

	public static final byte[] v2sig = {0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A};
	public static final byte[] v1sig = "PROXY ".getBytes(StandardCharsets.ISO_8859_1);

	/**
	 * bb 必须有足够的数据
	 *
	 * @param bb 数据buffer
	 * @return true start with v2sig
	 */
	public static boolean startWithV2sig(byte [] bb, int offset) {
		return Arrays.equals(bb, offset, offset + v2sig.length, v2sig, 0, v2sig.length);
	}

	public static boolean startWithV1sig(byte [] bb, int offset) {
		return Arrays.equals(bb, offset, offset + v1sig.length, v1sig, 0, v1sig.length);
	}

	// 返回 null 表示尚未收到 CRLF；返回空串表示收到空行（畸形头）。
	public static @Nullable String findV1Line(byte [] bytes, int offset, int end) {
		end--;
		for (int i = offset; i < end; i++) {
			if (bytes[i] == '\r' && bytes[i + 1] == '\n')
				return new String(bytes, offset, i - offset, StandardCharsets.ISO_8859_1);
		}
		return null;
	}

	private static void checkV2AddressLength(int size, int addressLength) {
		if (size - 16 < addressLength)
			throw new RuntimeException("haproxy v2 address length " + (size - 16) + " < " + addressLength);
	}

	// 保留 throws UnknownHostException 仅用于兼容既有调用方签名。
	public boolean decodeHeader(ByteBuffer bb) throws UnknownHostException {
		if (done)
			return true;

		// 16 = v2gig(12) + version_command(1) + family(1) + length(2)
		if (bb.size() >= 16 && startWithV2sig(bb.Bytes, bb.ReadIndex) && ((bb.Bytes[bb.ReadIndex + v2sig.length] & 0xF0) == 0x20)) {
			var javaBb = java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.size());
			javaBb.order(ByteOrder.BIG_ENDIAN);
			int size = 16 + (javaBb.getShort(bb.ReadIndex + 14) & 0xffff); // offset 14 是个 uint16_t，需要 ntohs。
			if (bb.size() < size)
				return false; // not enough data
			int cmd = bb.Bytes[bb.ReadIndex + v2sig.length] & 0xF;
			switch (cmd) {
			case 0x01: // PROXY command
				int fam = bb.Bytes[bb.ReadIndex + v2sig.length + 1];
				switch (fam) {
				case 0x11: // TCPv4
					checkV2AddressLength(size, 12); // 4+4地址 + 2+2端口
					// port读出来，再拼成InetSocketAddress。拼成Inet，就不需要单独保存了。
					var remoteInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 20));
					remoteAddress = new InetSocketAddress(remoteInet4Address, javaBb.getShort(bb.ReadIndex + 24) & 0xffff); // 端口是uint16
					var targetInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 20, bb.ReadIndex + 24));
					targetAddress = new InetSocketAddress(targetInet4Address, javaBb.getShort(bb.ReadIndex + 26) & 0xffff); // 端口是uint16
					break;
				case 0x21: // TCPv6
					checkV2AddressLength(size, 36); // 16+16地址 + 2+2端口
					var remoteInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 32));
					remoteAddress = new InetSocketAddress(remoteInet6Address, javaBb.getShort(bb.ReadIndex + 48) & 0xffff); // 端口是uint16
					var targetInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 32, bb.ReadIndex + 48));
					targetAddress = new InetSocketAddress(targetInet6Address, javaBb.getShort(bb.ReadIndex + 50) & 0xffff); // 端口是uint16
					break;
				default:
					throw new RuntimeException("haproxy not a supported transport family: "
							+ Integer.toHexString(fam & 0xff));
				}
				break;
			case 0x00: // LOCAL command
				// keep local connection address for LOCAL
				break;
			default:
				throw new RuntimeException("haproxy not a supported command");
			}

			bb.ReadIndex += size;
			done = true;
			return true;
		}

		if (bb.size() >= 8 && startWithV1sig(bb.Bytes, bb.ReadIndex)) { // PROXY ...\r\n
			var line = findV1Line(bb.Bytes, bb.ReadIndex + v1sig.length, bb.WriteIndex);
			if (line == null) {
				if (bb.size() > 107)
					throw new RuntimeException("haproxy v1 line too long");
				return false;
			}
			if (v1sig.length + line.length() + 2 > 107) // 规范限定 v1 整行最长 107 字节
				throw new RuntimeException("haproxy v1 line too long");
			if (line.isEmpty())
				throw new RuntimeException("haproxy v1 empty line");
			var tokens = line.split(" ");
			if (tokens.length >= 5) {
				switch (tokens[0]) {
				case "TCP4":
				case "TCP6":
					remotePort = parsePort(tokens[3]);
					remoteHost = tokens[1];
					targetPort = parsePort(tokens[4]);
					targetHost = tokens[2];
					break;
				default:
					throw new RuntimeException("haproxy v1 unsupported family: " + tokens[0]);
				}
			} else if (!line.equals("UNKNOWN"))
				throw new RuntimeException("haproxy v1 incomplete family line: " + line);
			bb.ReadIndex += v1sig.length + line.length() + 2; // 跳过"PROXY "前缀、line和line后的CRLF
			done = true;
			return true;
		}

		if (bb.size() >= 16)
			throw new RuntimeException("haproxy wrong protocol");
		return false;
	}

	private static int parsePort(String token) {
		int port;
		try {
			port = Integer.parseInt(token);
		} catch (NumberFormatException e) {
			throw new RuntimeException("haproxy v1 port not a number: " + token);
		}
		if (port < 0 || port > 65535)
			throw new RuntimeException("haproxy v1 port out of range: " + port);
		return port;
	}
}
