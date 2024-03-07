package Zeze.Net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Helper {
	public static final int MAX_BUFFER_SIZE = 0x4000_0000; // 1G

	/**
	 * @param size size
	 * @return 符合 2^n 并且不小于size
	 * @throws IllegalArgumentException if size < 0 || size > MAX_BUFFER_SIZE
	 */
	public static int roundup(int size) {
		if (Integer.compareUnsigned(size, MAX_BUFFER_SIZE) > 0)
			throw new IllegalArgumentException("Helper.roundup size=" + size);
		int capacity = 16;
		while (size > capacity)
			capacity <<= 1;
		return capacity;
	}

	/**
	 * 重新分配内存。 当 buffer 的剩余空间不足increment时，扩展buffer内存。 分配内存的时候按 roundup 方式增长。
	 *
	 * @param buffer    当前buffer，可以为null。
	 * @param increment 需要增加的空间。
	 * @return 剩余空间足够的buffer。
	 */
	public static @NotNull ByteBuffer realloc(@Nullable ByteBuffer buffer, int increment) {
		if (null == buffer) {
			return ByteBuffer.allocate(roundup(increment));
		}

		if (buffer.remaining() < increment) {
			buffer.flip(); // prepare to read
			return ByteBuffer.allocate(roundup(buffer.limit() + increment)).put(buffer);
		}

		return buffer;
	}

	/**
	 * 检查数组使用是否越界。
	 * <p>
	 * 可用在这样的实现里面： get(byte [] dst, int offset, int length)。
	 * <p>
	 * jdk的实现都有此检查，如果我们的实现只是调用jdk，就不用特别检查。 只有我们需要分批填写dst，为了避免前面的填写成功，而后面填写失败，
	 * 最好在开始填写dst使用这个方法之前提前检查边界。
	 * <p>
	 * 需要时再开放这个方法，现在仅用于包内。
	 *
	 * @param off  offset
	 * @param len  length
	 * @param size size
	 */
	public static void checkBounds(int off, int len, int size) {
		if ((off | len | (off + len) | (size - (off + len))) < 0)
			throw new IndexOutOfBoundsException();
	}

	/**
	 * 把整数形式保存的 IpAddress 和 port 转换成 InetSocketAddress。
	 */
	public static @NotNull InetSocketAddress inetSocketAddress(int address, int port) {
		return new InetSocketAddress(inetAddress(address), port);
	}

	/**
	 * 把 InetAddress 转换保存在 int(大端) 中。
	 *
	 * @throws IllegalArgumentException if addr is not a ip4 address
	 */
	public static int ip4(@NotNull InetAddress addr) {
		byte[] addrs = addr.getAddress();
		if (addrs.length != 4)
			throw new IllegalArgumentException(addr + " is not a ip4 address");
		return (int)Zeze.Serialize.ByteBuffer.intBeHandler.get(addrs, 0);
	}

	/**
	 * 把整数(大端)形式保存的 IpAddress 转换成 InetAddress。
	 */
	public static @NotNull InetAddress inetAddress(int address) {
		try {
			byte[] addr = new byte[4];
			Zeze.Serialize.ByteBuffer.intBeHandler.set(addr, 0, address);
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	// 域名解析
	public static @NotNull InetAddress toInetAddress(@Nullable String hostOrAddress) throws UnknownHostException {
		return InetAddress.getByName(hostOrAddress);
	}

	private static int getAddressLevel(@Nullable InetAddress addr, boolean preferInternal) {
		if (addr instanceof Inet4Address) {
			if (addr.isSiteLocalAddress()) // 局域网
				return preferInternal ? 0 : 2;
			if (addr.isLoopbackAddress()) // 本机
				return 4;
			return preferInternal ? 2 : 0;
		}
		if (addr instanceof Inet6Address) {
			if (addr.isSiteLocalAddress()) // 局域网
				return preferInternal ? 1 : 3;
			if (addr.isLoopbackAddress()) // 本机
				return 5;
			return preferInternal ? 3 : 1;
		}
		return 6;
	}

	// preferInternal=true 优先级: IPv4 SiteLocal > IPv6 SiteLocal > IPv4 External > IPv6 External > IPv4 Loopback > IPv6 Loopback
	// preferInternal=false优先级: IPv4 External > IPv6 External > IPv4 SiteLocal > IPv6 SiteLocal > IPv4 Loopback > IPv6 Loopback
	public static @NotNull String selectOneIpAddress(boolean preferInternal) {
		try {
			InetAddress bestAddr = null;
			int bestLevel = 6;
			for (var nis = NetworkInterface.getNetworkInterfaces(); nis.hasMoreElements(); ) {
				for (var addrs = nis.nextElement().getInetAddresses(); addrs.hasMoreElements(); ) {
					var addr = addrs.nextElement();
					int level = getAddressLevel(addr, preferInternal);
					if (level < bestLevel) {
						bestAddr = addr;
						bestLevel = level;
					}
				}
			}
			return bestAddr != null ? bestAddr.getHostAddress() : "";
		} catch (Exception e) {
			Task.forceThrow(e);
			return ""; // never run here
		}
	}

	// 枚举并输出本机的所有网卡的所有IP地址
	public static void main(String[] args) throws Exception {
		System.out.println("@internal: " + selectOneIpAddress(true));
		System.out.println("@external: " + selectOneIpAddress(false));
		System.out.println("Loopback : " + InetAddress.getLoopbackAddress());
		System.out.println("LocalHost: " + InetAddress.getLocalHost().getHostAddress());
		for (var nis = NetworkInterface.getNetworkInterfaces(); nis.hasMoreElements(); ) {
			var ni = nis.nextElement();
			System.out.println(ni.getIndex() + ": " + ni.getName() + ", " + ni.getDisplayName());
			for (var addrs = ni.getInetAddresses(); addrs.hasMoreElements(); ) {
				var addr = addrs.nextElement();
				System.out.println("  " + addr.getClass().getSimpleName() + ": " + addr
						+ ", Loopback=" + addr.isLoopbackAddress() + ", SiteLocal=" + addr.isSiteLocalAddress());
			}
		}
	}
}
