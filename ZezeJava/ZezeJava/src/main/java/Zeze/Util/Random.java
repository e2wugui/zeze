package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Random {
	public static ThreadLocalRandom getInstance() {
		return ThreadLocalRandom.current();
	}

	public static Zeze.Net.Binary nextBinary(int size) {
		var rand = getInstance();
		var bytes = new byte[size];
		rand.nextBytes(bytes);
		return new Zeze.Net.Binary(bytes);
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

	public static <T> T[] shuffle(T[] list) {
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
