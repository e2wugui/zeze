package Zeze.Util;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Net.Binary;
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

	public static long hash64(long hash64, byte @NotNull [] bytes, int beginIndex, int endIndex) {
		for (int i = beginIndex; i < endIndex; i++)
			hash64 = (hash64 + bytes[i]) * 3074457345618258799L;
		return hash64;
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
		wLock.lock();
		try {
			pool.put(hash64, b = new Binary(Arrays.copyOfRange(bytes, beginIndex, endIndex)));
		} finally {
			wLock.unlock();
		}
		return b;
	}
}
