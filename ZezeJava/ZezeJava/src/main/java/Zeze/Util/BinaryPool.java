package Zeze.Util;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

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

	public @NotNull Binary intern(byte @NotNull [] bytes) {
		return intern(bytes, 0, bytes.length);
	}

	public @NotNull Binary intern(byte @NotNull [] bytes, int beginIndex, int endIndex) {
		var hash64 = hash64(HASH_BASE, bytes, beginIndex, endIndex);
		Binary b;
		rLock.lock();
		try {
			var h64 = hash64;
			for (int i = 0; i < 32; i++) { // 超过32的冲突可能性极低,一旦遇到就覆盖掉吧
				b = pool.get(h64);
				if (b == null) {
					hash64 = h64;
					break;
				}
				if (Arrays.equals(b.bytesUnsafe(), 0, b.size(), bytes, beginIndex, endIndex))
					return b;
				h64 = hash64(HASH_BASE + i, bytes, beginIndex, endIndex);
			}
		} finally {
			rLock.unlock();
		}
		b = new Binary(Arrays.copyOfRange(bytes, beginIndex, endIndex));
		wLock.lock();
		try {
			pool.put(hash64, b);
		} finally {
			wLock.unlock();
		}
		return b;
	}

	public @NotNull Binary intern(@NotNull java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		var hash64 = hash64(HASH_BASE, bb, beginIndex, endIndex);
		Binary b;
		rLock.lock();
		try {
			var h64 = hash64;
			for (int i = 0; i < 32; i++) { // 超过32的冲突可能性极低,一旦遇到就覆盖掉吧
				b = pool.get(h64);
				if (b == null) {
					hash64 = h64;
					break;
				}
				if (equals(b, bb, beginIndex, endIndex))
					return b;
				h64 = hash64(HASH_BASE + i, bb, beginIndex, endIndex);
			}
		} finally {
			rLock.unlock();
		}
		int n = endIndex - beginIndex;
		var bytes = new byte[n];
		// bb.get(beginIndex, bytes, 0, n);
		var p = bb.position();
		bb.position(beginIndex);
		bb.get(bytes, 0, n);
		bb.position(p);
		b = new Binary(bytes);
		wLock.lock();
		try {
			pool.put(hash64, b);
		} finally {
			wLock.unlock();
		}
		return b;
	}

	private static long hash64(long hash64, byte @NotNull [] bytes, int beginIndex, int endIndex) {
		for (int i = beginIndex; i < endIndex; i++)
			hash64 = (hash64 + bytes[i]) * 3074457345618258799L;
		return hash64;
	}

	private static long hash64(long hash64, @NotNull java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		for (int i = beginIndex; i < endIndex; i++)
			hash64 = (hash64 + bb.get(i)) * 3074457345618258799L;
		return hash64;
	}

	private static boolean equals(@NotNull Binary b, @NotNull java.nio.ByteBuffer bb, int beginIndex, int endIndex) {
		int n = b.size();
		if (n != endIndex - beginIndex)
			return false;
		var bytes = b.bytesUnsafe();
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int i = 0;
		for (; i + 8 <= n; i += 8) {
			if (ByteBuffer.ToLong(bytes, i) != bb.getLong(beginIndex + i))
				return false;
		}
		if (i + 4 <= n) {
			if (ByteBuffer.ToInt(bytes, i) != bb.getInt(beginIndex + i))
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
