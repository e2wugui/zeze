package Zeze.Util;

import java.util.Arrays;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 字符串模糊匹配算法. 使用1-2-3-4哈希的倒排索引
 */
public final class StringFuzzySearch {
	private static final boolean DEBUG = true;
	private long sidCounter;
	private final LongHashMap<String> strMap = new LongHashMap<>(); // key:sid
	private final HashMap<String, Long> strSet = new HashMap<>(); // value:sid
	private final IntHashMap<LongList> index1 = new IntHashMap<>(); // high 16 bits: weight; low 48 bits: sid
	private final IntHashMap<LongList> index2 = new IntHashMap<>();
	private final LongHashMap<LongList> index3 = new LongHashMap<>();
	private final LongHashMap<LongList> index4 = new LongHashMap<>();

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
		var k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16);
		for (int i = 2; i < n; i++) {
			k = ((k << 16) + s.charAt(i)) & 0xffff_ffff_ffffL;
			index3.computeIfAbsent(k, __ -> new LongList()).add(v);
		}
		if (n < 4)
			return true;
		k = ((long)s.charAt(0) << 48) + ((long)s.charAt(1) << 32) + ((long)s.charAt(2) << 16);
		for (int i = 3; i < n; i++) {
			k = (k << 16) + s.charAt(i);
			index4.computeIfAbsent(k, __ -> new LongList()).add(v);
		}
		return true;
	}

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
		var k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16);
		for (int i = 2; i < n; i++) {
			k = ((k << 16) + s.charAt(i)) & 0xffff_ffff_ffffL;
			final var list = index3.get(k);
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		if (n < 4)
			return true;
		k = ((long)s.charAt(0) << 48) + ((long)s.charAt(1) << 32) + ((long)s.charAt(2) << 16);
		for (int i = 3; i < n; i++) {
			k = (k << 16) + s.charAt(i);
			final var list = index4.get(k);
			//noinspection DataFlowIssue
			list.removeAndExchangeLast(list.indexOf(v));
		}
		return true;
	}

	public int search(final @NotNull String s, final @NotNull String @NotNull [] res) {
		final int max = res.length;
		final var m = new LongHashMap<OutInt>();
		final int n = s.length();
		if (n >= 4) {
			var k = ((long)s.charAt(0) << 48) + ((long)s.charAt(1) << 32) + ((long)s.charAt(2) << 16);
			for (int i = 3; i < n; i++) {
				k = (k << 16) + s.charAt(i);
				merge(m, index4.get(k), 16);
			}
		}
		if (n >= 3 && m.size() < max) {
			var k = ((long)s.charAt(0) << 32) + ((long)s.charAt(1) << 16);
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
	public static void main(final String[] args) {
		final var sfs = new StringFuzzySearch();
		sfs.add("abc");
		sfs.add("abcdef");
		sfs.add("abcabc");
		sfs.add("bcd");
		sfs.add("a");
		System.out.println(sfs.search("abc", new String[5]));
		System.out.println("--------");
		System.out.println(sfs.search("def", new String[5]));
	}
}
