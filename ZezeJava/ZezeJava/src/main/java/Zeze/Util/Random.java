package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

public final class Random {
	public static @NotNull ThreadLocalRandom getInstance() {
		return ThreadLocalRandom.current();
	}

	public static @NotNull Zeze.Net.Binary nextBinary(int size) {
		var rand = getInstance();
		var bytes = new byte[size];
		rand.nextBytes(bytes);
		return new Zeze.Net.Binary(bytes);
	}

	public static <T> @NotNull List<T> shuffle(@NotNull List<T> list) {
		var random = getInstance();
		for (int i = 1, n = list.size(); i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list.get(i);
			list.set(i, list.get(pos));
			list.set(pos, x);
		}
		return list;
	}

	public static <T> T @NotNull [] shuffle(T @NotNull [] list) {
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
