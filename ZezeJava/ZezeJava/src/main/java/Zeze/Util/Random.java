package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

public final class Random {
	public static @NotNull ThreadLocalRandom getInstance() {
		return ThreadLocalRandom.current();
	}

	public static byte @NotNull [] nextBytes(int size) {
		var bytes = new byte[size];
		getInstance().nextBytes(bytes);
		return bytes;
	}

	public static @NotNull Binary nextBinary(int size) {
		return new Binary(nextBytes(size));
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

	public static int @NotNull [] shuffle(int @NotNull [] list) {
		var random = getInstance();
		for (int i = 1, n = list.length; i < n; i++) {
			int pos = random.nextInt(i + 1);
			var x = list[i];
			list[i] = list[pos];
			list[pos] = x;
		}
		return list;
	}

	public static long @NotNull [] shuffle(long @NotNull [] list) {
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
