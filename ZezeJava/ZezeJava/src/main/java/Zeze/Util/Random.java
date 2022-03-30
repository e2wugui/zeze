package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Random {
	public static java.util.Random getInstance() {
		return ThreadLocalRandom.current();
	}

	public static <T> List<T> Shuffle(List<T> list) {
		java.util.Random random = getInstance();
		for (int i = 1; i < list.size(); i++) {
			int pos = random.nextInt(i + 1);
			var x = list.get(i);
			list.set(i, list.get(pos));
			list.set(pos, x);
		}
		return list;
	}

	public static <T> T[] Shuffle(T[] list) {
		java.util.Random random = getInstance();
		for (int i = 1; i < list.length; i++) {
			int pos = random.nextInt(i + 1);
			var x = list[i];
			list[i] = list[pos];
			list[pos] = x;
		}
		return list;
	}

	public static void main(String[] args) {
		System.out.println(getInstance().nextLong());
	}
}
