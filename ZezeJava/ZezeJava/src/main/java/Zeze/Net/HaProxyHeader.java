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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 解析haproxy header，把信息保存下来。
 * 启用需要在Service里面配置。
 * Service会构造这个类并设置到AsyncSocket里面。
 * https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt
 */
public class HaProxyHeader {
	private static final @NotNull Logger logger = LogManager.getLogger(HaProxyHeader.class);

	private boolean done = false;
	private final String key;
	private volatile @Nullable InetSocketAddress remoteAddress;
	private volatile @Nullable InetSocketAddress targetAddress;
	// v1 懒解析：decodeHeader 在 selector 线程调用，InetAddress.getByName 对非字面量主机名会
	// 同步DNS解析（默认可达5-30秒），攻击者可用伪造的PROXY行卡死整个Selector线程上的所有连接。
	// 因此v1的TCP4/TCP6地址只在这里记录主机名和端口，推迟到getter首次访问时再解析。
	private volatile @Nullable String remoteHost;
	private volatile @Nullable String targetHost;
	private int remotePort;
	private int targetPort;

	public HaProxyHeader(String key) {
		this.key = key;
	}

	public String getKey() {
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

	private static @Nullable InetSocketAddress resolve(@NotNull String host, int port) {
		try {
			// 字面量IP不查DNS；主机名会同步解析，调用方线程需容忍可能的阻塞（但绝不能是selector线程）。
			return new InetSocketAddress(InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
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
	public static boolean startWithV2sig(byte @NotNull [] bb, int offset) {
		return Arrays.equals(bb, offset, offset + v2sig.length, v2sig, 0, v2sig.length);
	}

	public static boolean startWithV1sig(byte @NotNull [] bb, int offset) {
		return Arrays.equals(bb, offset, offset + v1sig.length, v1sig, 0, v1sig.length);
	}

	public static @NotNull String findV1Line(byte @NotNull [] bytes, int offset, int end) {
		end--;
		for (int i = offset; i < end; i++) {
			if (bytes[i] == '\r' && bytes[i + 1] == '\n')
				return new String(bytes, offset, i - offset, StandardCharsets.ISO_8859_1);
		}
		return "";
	}

	// 保留throws UnknownHostException仅为兼容既有调用方的catch，本方法已不再抛出（解析移到getter）。
	public boolean decodeHeader(@NotNull ByteBuffer bb) throws UnknownHostException {
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
					// port读出来，再拼成InetSocketAddress吧。当然拼成Inet，就不需要单独保存了。
					var remoteInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 20));
					remoteAddress = new InetSocketAddress(remoteInet4Address, javaBb.getShort(bb.ReadIndex + 24) & 0xffff); // 端口是uint16
					var targetInet4Address = Inet4Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 20, bb.ReadIndex + 24));
					targetAddress = new InetSocketAddress(targetInet4Address, javaBb.getShort(bb.ReadIndex + 26) & 0xffff); // 端口是uint16
					break;
				case 0x21: // TCPv6
					var remoteInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 16, bb.ReadIndex + 32));
					remoteAddress = new InetSocketAddress(remoteInet6Address, javaBb.getShort(bb.ReadIndex + 48) & 0xffff); // 端口是uint16
					var targetInet6Address = Inet6Address.getByAddress(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex + 32, bb.ReadIndex + 48));
					targetAddress = new InetSocketAddress(targetInet6Address, javaBb.getShort(bb.ReadIndex + 50) & 0xffff); // 端口是uint16
					break;
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
			if (line.isEmpty()) {
				if (bb.size() > 107)
					throw new RuntimeException("haproxy v1 line too long");
				return false;
			}
			// parse the V1 header using favorite address parsers like inet_pton.
			var tokens = line.split(" ");
			if (tokens.length >= 5) {
				switch (tokens[0]) {
				case "TCP4":
				case "TCP6":
					// 两个协议都用InetAddress.getByName，实现内部会区分。
					// 【N1-3】不在selector线程解析（见字段remoteHost处的说明），只记录，getter懒解析。
					remotePort = Integer.parseInt(tokens[3]);
					remoteHost = tokens[1];
					targetPort = Integer.parseInt(tokens[4]);
					targetHost = tokens[2];
					break;
				}
			}
			bb.ReadIndex += v1sig.length + line.length() + 2; // 跳过"PROXY "前缀、line和line后的CRLF
			done = true;
			return true;
		}

		if (bb.size() >= 16)
			throw new RuntimeException("haproxy wrong protocol");
		return false;
	}
}
