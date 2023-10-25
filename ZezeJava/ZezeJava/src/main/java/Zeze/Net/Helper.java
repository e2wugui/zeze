package Zeze.Net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class Helper {
	public static final int MAX_BUFFER_SIZE = 0x4000_0000; // 1G

	/**
	 * @param size size
	 * @return 符合 2^n 并且不小于size
	 * @throws IllegalArgumentException if size < 0 || size > 0x4000_0000
	 */
	public static int roundup(int size) {
		if (size < 0 || size > MAX_BUFFER_SIZE)
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
	public static ByteBuffer realloc(ByteBuffer buffer, int increment) {
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
	public static InetSocketAddress inetSocketAddress(int address, int port) {
		return new InetSocketAddress(inetAddress(address), port);
	}

	/**
	 * 把 InetAddress 转换保存在 int(大端) 中。
	 *
	 * @throws IllegalArgumentException if addr is not a ip4 address
	 */
	public static int ip4(InetAddress addr) {
		byte[] addrs = addr.getAddress();
		if (addrs.length != 4)
			throw new IllegalArgumentException(addr + " is not a ip4 address");
		return (int)Zeze.Serialize.ByteBuffer.intBeHandler.get(addrs, 0);
	}

	/**
	 * 把整数(大端)形式保存的 IpAddress 转换成 InetAddress。
	 */
	public static InetAddress inetAddress(int address) {
		try {
			byte[] addr = new byte[4];
			Zeze.Serialize.ByteBuffer.intBeHandler.set(addr, 0, address);
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static boolean isIp4(InetAddress address) {
		return address.getAddress().length == 4;
	}

	public static boolean isIp6(InetAddress address) {
		return address.getAddress().length == 16;
	}

	public static InetAddress toInetAddress(String hostOrAddress) throws UnknownHostException {
		return InetAddress.getByName(hostOrAddress);
	}

	public static boolean isPrivateAddress(InetAddress address) {
		if (isIp4(address)) {
			return isPrivateIPv4(address.toString());
		}
		if (isIp6(address)) {
			return isPrivateIPv6(address.toString());
		}
		throw new UnsupportedOperationException("Unknown InetAddress! address=" + address);
	}

	public static boolean isPrivateIPv4(String ipAddress) {
		try {
			String[] ipAddressArray = ipAddress.split("\\.");
			int[] ipParts = new int[ipAddressArray.length];
			for (int i = 0; i < ipAddressArray.length; i++) {
				ipParts[i] = Integer.parseInt(ipAddressArray[i].trim());
			}

			switch (ipParts[0]) {
			case 10:
			case 127:
				return true;
			case 172:
				return (ipParts[1] >= 16) && (ipParts[1] < 32);
			case 192:
				return (ipParts[1] == 168);
			case 169:
				return (ipParts[1] == 254);
			}
		} catch (Exception ignored) {
		}

		return false;
	}

	public static boolean isPrivateIPv6(String ipAddress) {
		boolean isPrivateIPv6 = false;
		String[] ipParts = ipAddress.trim().split("_");
		if (ipParts.length > 0) {
			String firstBlock = ipParts[0];
			String prefix = firstBlock.substring(0, 2);

			if (firstBlock.equalsIgnoreCase("fe80")
					|| firstBlock.equalsIgnoreCase("100")
					|| ((prefix.equalsIgnoreCase("fc") && firstBlock.length() >= 4))
					|| ((prefix.equalsIgnoreCase("fd") && firstBlock.length() >= 4))) {
				isPrivateIPv6 = true;
			}
		}
		return isPrivateIPv6;
	}

	public static String getOneNetworkInterfaceIpAddress() {
		try {
			var interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				var inetAddresses = interfaces.nextElement().getInetAddresses();
				if (inetAddresses.hasMoreElements())
					return inetAddresses.nextElement().getHostAddress();
			}
			return "";
		} catch (SocketException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static @NotNull String getOnePrivateNetworkInterfaceIpAddress() {
		try {
			var interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				var inetAddresses = interfaces.nextElement().getInetAddresses();
				if (inetAddresses.hasMoreElements()) {
					var address = inetAddresses.nextElement();
					if (isPrivateAddress(address)) {
						var ip = address.getHostAddress();
						if (ip != null && !ip.isBlank())
							return ip;
					}
				}
			}
			return "";
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static @NotNull String getOnePublicNetworkInterfaceIpAddress() {
		try {
			var interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				var inetAddresses = interfaces.nextElement().getInetAddresses();
				if (inetAddresses.hasMoreElements()) {
					var address = inetAddresses.nextElement();
					if (!isPrivateAddress(address)) {
						var ip = address.getHostAddress();
						if (ip != null && !ip.isBlank())
							return ip;
					}
				}
			}
			return "";
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}
}
