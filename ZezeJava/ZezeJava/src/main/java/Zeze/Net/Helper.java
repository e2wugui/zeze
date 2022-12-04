package Zeze.Net;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public final class Helper {
	public static final int MAX_BUFFER_SIZE = 0x40000000; // 1G

	/**
	 * @param size size
	 * @throws IllegalArgumentException if size < 0 || size >= 0x40000000
	 * @return 符合 2^n 并且不小于size
	 */
	public static int roudup(int size) {
		if (size < 0 || size > MAX_BUFFER_SIZE)
			throw new IllegalArgumentException("xio.Helper.roundup size=" + size);
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
			return ByteBuffer.allocate(roudup(increment));
		}

		if (buffer.remaining() < increment) {
			buffer.flip(); // prepare to read
			return ByteBuffer.allocate(roudup(buffer.limit() + increment)).put(buffer);
		}

		return buffer;
	}

	/**
	 * 检查数组使用是否越界。
	 * 
	 * 可用在这样的实现里面： get(byte [] dst, int offset, int length)。
	 *
	 * jdk的实现都有此检查，如果我们的实现只是调用jdk，就不用特别检查。 只有我们需要分批填写dst，为了避免前面的填写成功，而后面填写失败，
	 * 最好在开始填写dst使用这个方法之前提前检查边界。
	 * 
	 * 需要时再开放这个方法，现在仅用于包内。
	 * 
	 * @param off offset
	 * @param len length
	 * @param size size
	 */
	static void checkBounds(int off, int len, int size) {
		if ((off | len | (off + len) | (size - (off + len))) < 0)
			throw new IndexOutOfBoundsException();
	}

	/**
	 * 把整数形式保存的 IpAddress 和 port 转换成 InetSocketAddress。
	 */
	public static java.net.InetSocketAddress inetSocketAddress(int address, int port) {
		return new java.net.InetSocketAddress(inetAddress(address), port);
	}

	/**
	 * 把 InetAddress 转换保存在 int 中。
	 * 
	 * @throws RuntimeException if addr is not a ip4 address
	 */
	public static int ip4(java.net.InetAddress addr) {
		int ip = 0;
		byte[] addrs = addr.getAddress();
		if (addrs.length != 4)
			throw new RuntimeException(addr + " is not a ip4 address");
		ip |= (addrs[3] << 24) & 0xff000000;
		ip |= (addrs[2] << 16) & 0x00ff0000;
		ip |= (addrs[1] << 8) & 0x0000ff00;
		ip |= (addrs[0]) & 0x000000ff;
		return ip;
	}

	/**
	 * 把整数形式保存的 IpAddress 转换成 InetAddress。
	 */
	public static java.net.InetAddress inetAddress(int address) {
		try {
			byte[] addr = new byte[4];
			addr[0] = (byte) ((address >>> 24) & 0xFF);
			addr[1] = (byte) ((address >>> 16) & 0xFF);
			addr[2] = (byte) ((address >>> 8) & 0xFF);
			addr[3] = (byte) (address & 0xFF);
			return java.net.InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
