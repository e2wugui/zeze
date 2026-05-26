package Zeze.Util;

import java.util.Arrays;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 字符串模糊匹配算法. 使用1-2-3-4哈希的倒排索引
 */
public final class StringFuzzySearch {
	public static boolean DEBUG = true;
	private long sidCounter;
	private final LongHashMap<String> strMap = new LongHashMap<>(); // key:sid
	private final HashMap<String, Long> strSet = new HashMap<>(); // value:sid
	private final IntHashMap<LongList> index1 = new IntHashMap<>(); // value: weight(high 16 bits) + sid (low 48 bits)
	private final IntHashMap<LongList> index2 = new IntHashMap<>();
	private final LongHashMap<LongList> index3 = new LongHashMap<>();
	private final LongHashMap<LongList> index4 = new LongHashMap<>();

	// 只读,可加读锁
	public int size() {
		return strMap.size();
	}

	// 修改,需要加写锁
	public boolean add(final @NotNull String s) {
		final int n = s.length();
		if (n <= 0 || strSet.containsKey(s))
			return false;
		final var sid = ++sidCounter;
		strMap.put(sid, s);
		strSet.put(s, sid);
		final var v = ((long)(0xfff / n) << 48) + sid;
		for (int i = 0; i < n; i++)
			index1.computeIfAbsent(s.charAt(i), __ -> new LongList()).add(v);
		int j = s.charAt(0);
		for (int i = 1; i < n; i++) {
			j = (j << 16) + s.charAt(i);
			index2.computeIfAbsent(j, __ -> new LongList()).add(v);
		}
		if (n < 3)
			return true;
		var k = ((long)s.charAt(0) << 16) + (long)s.charAt(1);
		for (int i = 2; i < n; i++) {
			k = ((k << 16) + s.charAt(i)) & 0xffff_ffff_ffffL;
			index3.computeIfAbsent(k, __ -> new LongList()).add(v);
		}
		if (n < 4)
			return true;
		k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16) + (long)s.charAt(2);
		for (int i = 3; i < n; i++) {
			k = (k << 16) + s.charAt(i);
			index4.computeIfAbsent(k, __ -> new LongList()).add(v);
		}
		return true;
	}

	// 修改,需要加写锁
	public boolean remove(final @NotNull String s) {
		final var obj = strSet.remove(s);
		if (obj == null)
			return false;
		final long sid = obj;
		strMap.remove(sid);
		final int n = s.length();
		final var v = ((long)(0xfff / n) << 48) + sid;
		for (int i = 0; i < n; i++) {
			final var list = index1.get(s.charAt(i));
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		int j = s.charAt(0);
		for (int i = 1; i < n; i++) {
			j = (j << 16) + s.charAt(i);
			final var list = index2.get(j);
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		if (n < 3)
			return true;
		var k = ((long)s.charAt(0) << 16) + (long)s.charAt(1);
		for (int i = 2; i < n; i++) {
			k = ((k << 16) + s.charAt(i)) & 0xffff_ffff_ffffL;
			final var list = index3.get(k);
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		if (n < 4)
			return true;
		k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16) + (long)s.charAt(2);
		for (int i = 3; i < n; i++) {
			k = (k << 16) + s.charAt(i);
			final var list = index4.get(k);
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		return true;
	}

	// 只读,可加读锁
	public int search(final @NotNull String s, final @NotNull String @NotNull [] res) {
		final int max = res.length;
		final var m = new LongHashMap<OutInt>();
		final int n = s.length();
		if (n >= 4) {
			var k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16) + (long)s.charAt(2);
			for (int i = 3; i < n; i++) {
				k = (k << 16) + s.charAt(i);
				merge(m, index4.get(k), 16);
			}
		}
		if (n >= 3 && m.size() < max) {
			var k = (long)s.charAt(0) << 16 + (long)s.charAt(1);
			for (int i = 2; i < n; i++) {
				k = ((k << 16) + s.charAt(i)) & 0xffff_ffff_ffffL;
				merge(m, index3.get(k), 9);
			}
		}
		if (n >= 2 && m.size() < max) {
			int j = s.charAt(0);
			for (int i = 1; i < n; i++) {
				j = (j << 16) + s.charAt(i);
				merge(m, index2.get(j), 4);
			}
		}
		if (n >= 1 && m.size() < max) {
			for (int i = 0; i < n; i++)
				merge(m, index1.get(s.charAt(i)), 1);
		}
		int i = 0, e = m.size();
		final var is = new long[e];
		final var f = 0x1_0000_0000L / n;
		for (final var it = m.iterator(); it.moveToNext(); )
			is[i++] = (it.key() & 0xffff_ffff_ffffL) + (Math.min((it.value().value * f) >>> 32, 0xffff) << 48);
		Arrays.sort(is);
		for (i = 0; i < max && --e >= 0; i++) {
			final var v = is[e];
			//noinspection DataFlowIssue
			res[i] = strMap.get(v & 0xffff_ffff_ffffL);
			if (DEBUG)
				System.out.format("%5d %s\n", v >>> 48, res[i]);
		}
		return i;
	}

	private static void merge(final @NotNull LongHashMap<OutInt> m, final @Nullable LongList list, final int f) {
		if (list != null) {
			for (int i = 0, n = list.size(); i < n; i++) {
				final var k = list.get(i);
				m.computeIfAbsent(k, __ -> new OutInt()).value += (int)(k >>> 48) * f;
			}
		}
	}

	@SuppressWarnings("SpellCheckingInspection")
	public static void main(final String[] args) throws Exception {
		final var sfs = new StringFuzzySearch();
		sfs.add("abc");
		sfs.add("abcdef");
		sfs.add("abcabc");
		sfs.add("bcd");
		sfs.add("a");
		System.out.println(sfs.search("abc", new String[5]));
		System.out.println("--------");
		System.out.println(sfs.search("def", new String[5]));
/*
		// 测试了一套网名库,813026条,9.75M(UTF-8编码,\n换行,中英文混合,中文较多)
		// 加载时长2.2秒(i7-8700),占JVM堆540M左右,预热后搜索一条不到0.1毫秒
		var t0 = System.nanoTime();
		for (var line : Files.readAllLines(java.nio.file.Path.of("names.txt"), StandardCharsets.UTF_8))
			sfs.add(line);
		t0 = System.nanoTime() - t0;
		System.gc();
		for (int i = 0; i < 1000; i++) {
			System.out.println("加载时间: " + t0 / 1_000_000 + "毫秒, 条目数: " + sfs.size());
			var t = System.nanoTime();
			System.out.println("搜索: zeze");
			System.out.println("结果:");
			sfs.search("zeze", new String[5]);
			System.out.println("搜索时长: " + (System.nanoTime() - t) + "纳秒");
			System.out.println("--------");
			t = System.nanoTime();
			System.out.println("搜索: 菠萝小王子");
			sfs.search("菠萝小王子", new String[5]);
			System.out.println("搜索时长: " + (System.nanoTime() - t) + "纳秒");
			System.out.println("--------");
			t = System.nanoTime();
			System.out.println("搜索: 天下第一网名");
			sfs.search("天下第一网名", new String[5]);
			System.out.println("搜索时长: " + (System.nanoTime() - t) + "纳秒");
			System.out.println("--------");
		}
		System.in.read();
*/
	}
}
