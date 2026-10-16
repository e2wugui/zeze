package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Net.Binary;

// 随机数工具集：ThreadLocalRandom 入口与 byte/Binary 等随机填充
public final class Random {
	public static ThreadLocalRandom getInstance() {
		return ThreadLocalRandom.current();
	}

	public static byte [] nextBytes(int size) {
		var bytes = new byte[size];
		getInstance().nextBytes(bytes);
		return bytes;
	}

	public static Binary nextBinary(int size) {
		return new Binary(nextBytes(size));
	}

	public static <T> List<T> shuffle(List<T> list) {
		var random = getInstance();
		for (int i = 1, n = list.size(); i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list.get(i);
			list.set(i, list.get(pos));
			list.set(pos, x);
		}
		return list;
	}

	public static <T> T [] shuffle(T [] list) {
		var random = getInstance();
		for (int i = 1, n = list.length; i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list[i];
			list[i] = list[pos];
			list[pos] = x;
		}
		return list;
	}

	public static int [] shuffle(int [] list) {
		var random = getInstance();
		for (int i = 1, n = list.length; i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list[i];
			list[i] = list[pos];
			list[pos] = x;
		}
		return list;
	}

	public static long [] shuffle(long [] list) {
		var random = getInstance();
		for (int i = 1, n = list.length; i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list[i];
			list[i] = list[pos];
			list[pos] = x;
		}
		return list;
	}
}
