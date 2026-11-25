package Zeze.Util;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

// Binary 内容去重池（intern）：同一内容返回同一引用，读写锁分护探测与插入
public class BinaryPool {
	private static final long HASH_BASE = 3074457345618258791L;
	private final LongHashMap<Binary> pool = new LongHashMap<>();
	private final ReentrantReadWriteLock.ReadLock rLock;
	private final ReentrantReadWriteLock.WriteLock wLock;

	public BinaryPool() {
		var rwLock = new ReentrantReadWriteLock();
		rLock = rwLock.readLock();
		wLock = rwLock.writeLock();
	}

	public Binary intern(byte [] bytes) {
		return intern(bytes, 0, bytes.length);
	}

	public Binary intern(byte [] bytes, int beginIndex, int endIndex) {
		final var baseHash = hash64(HASH_BASE, bytes, beginIndex, endIndex);
		var hash64 = baseHash;
		int probeStep = 0;
		Binary b;
		rLock.lock();
		try {
			var h64 = baseHash;
			for (int i = 0; ; ) { // 探测32槽（种子 HASH_BASE+0..31），超过32冲突极低，全满则覆盖首槽
				b = pool.get(h64);
				if (b == null) {
					hash64 = h64;
					probeStep = i;
					break;
				}
				if (Arrays.equals(b.bytesUnsafe(), 0, b.size(), bytes, beginIndex, endIndex))
					return b;
				if (++i >= 32) {
					probeStep = 32;
					break;
				}
				h64 = hash64(HASH_BASE + i, bytes, beginIndex, endIndex);
			}
		} finally {
			rLock.unlock();
		}
		b = new Binary(Arrays.copyOfRange(bytes, beginIndex, endIndex));
		return putUnderWriteLock(baseHash, hash64, probeStep, b);
	}

	public Binary intern(java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		final var baseHash = hash64(HASH_BASE, bb, beginIndex, endIndex);
		var hash64 = baseHash;
		int probeStep = 0;
		Binary b;
		rLock.lock();
		try {
			var h64 = baseHash;
			for (int i = 0; ; ) { // 探测32槽（种子 HASH_BASE+0..31），超过32冲突极低，全满则覆盖首槽
				b = pool.get(h64);
				if (b == null) {
					hash64 = h64;
					probeStep = i;
					break;
				}
				if (equals(b, bb, beginIndex, endIndex))
					return b;
				if (++i >= 32) {
					probeStep = 32;
					break;
				}
				h64 = hash64(HASH_BASE + i, bb, beginIndex, endIndex);
			}
		} finally {
			rLock.unlock();
		}
		int n = endIndex - beginIndex;
		var bytes = new byte[n];
		bb.get(beginIndex, bytes, 0, n);
		b = new Binary(bytes);
		return putUnderWriteLock(baseHash, hash64, probeStep, b);
	}

	// package-private：供单测确定性覆盖读锁→写锁窗口的并发冲突探测路径
	Binary putUnderWriteLock(long baseHash, long hash64, int probeStep, Binary b) {
		var bytes = b.bytesUnsafe();
		int len = b.size();
		wLock.lock();
		try {
			// 读锁释放到获取写锁期间可能有并发插入（相同内容或哈希冲突的不同内容）：
			// 若目标槽已非空且内容不等，沿探测链继续检查后续槽位，命中等值实例则复用，找到空槽则写入，
			// 仅 32 槽全满时按既有策略覆盖首槽（baseHash），避免直接覆写其他活条目。
			var exist = pool.get(hash64);
			if (exist != null) {
				if (Arrays.equals(exist.bytesUnsafe(), 0, exist.size(), bytes, 0, len))
					return exist;
				var targetHash = baseHash;
				for (int i = probeStep + 1; i < 32; i++) {
					var h64 = hash64(HASH_BASE + i, bytes, 0, len);
					exist = pool.get(h64);
					if (exist == null) {
						targetHash = h64;
						break;
					}
					if (Arrays.equals(exist.bytesUnsafe(), 0, exist.size(), bytes, 0, len))
						return exist;
				}
				hash64 = targetHash;
			}
			pool.put(hash64, b);
		} finally {
			wLock.unlock();
		}
		return b;
	}

	private static long hash64(long hash64, byte [] bytes, int beginIndex, int endIndex) {
		for (int i = beginIndex; i < endIndex; i++)
			hash64 = (hash64 + bytes[i]) * 3074457345618258799L;
		return hash64;
	}

	private static long hash64(long hash64, java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		for (int i = beginIndex; i < endIndex; i++)
			hash64 = (hash64 + bb.get(i)) * 3074457345618258799L;
		return hash64;
	}

	private static boolean equals(Binary b, java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		int n = b.size();
		if (n != endIndex - beginIndex)
			return false;
		var bytes = b.bytesUnsafe();
		// 按调用方字节序读取，再归一为 Zeze ByteBuffer 的小端值。
		boolean reverse = bb.order() == ByteOrder.BIG_ENDIAN;
		int i = 0;
		for (; i + 8 <= n; i += 8) {
			long value = bb.getLong(beginIndex + i);
			if (ByteBuffer.ToLong(bytes, i) != (reverse ? Long.reverseBytes(value) : value))
				return false;
		}
		if (i + 4 <= n) {
			int value = bb.getInt(beginIndex + i);
			if (ByteBuffer.ToInt(bytes, i) != (reverse ? Integer.reverseBytes(value) : value))
				return false;
			i += 4;
		}
		for (; i < n; i++) {
			if (bytes[i] != bb.get(beginIndex + i))
				return false;
		}
		return true;
	}
}
