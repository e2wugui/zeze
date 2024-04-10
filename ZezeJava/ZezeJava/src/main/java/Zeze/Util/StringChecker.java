package Zeze.Util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 字符串匹配大量固定子串的高效算法. 使用trie树结构搭配AC自动机算法, 内存占用大概是所有子串的30倍大小
 */
public final class StringChecker {
	private static final class Trie extends CharHashMap<Trie> {
		private int deep;
		private Trie fail;

		void add(@NotNull String str, int i, int e) {
			for (Trie trie = this, next; ; trie = next) {
				trie.deep = i;
				char c = str.charAt(i);
				next = trie.get(c);
				if (++i >= e) {
					if (next == null)
						trie.put(c, trie); // 放入this表示叶子节点(终止节点)
					else if (next != trie)
						next.put((char)0, null); // 或者把this放到0位置表示非叶子的终止节点
					return;
				}
				if (next == null)
					trie.put(c, next = new Trie());
				else if (next == trie) {
					trie.put(c, next = new Trie());
					next.put((char)0, null);
				}
			}
		}

		void calFail(@NotNull Trie root, char @NotNull [] stack, int i) {
			fail = root;
			for (int j = 1; j < i; j++) {
				Trie t = root;
				for (int k = j; k < i; k++) {
					t = t.get(stack[k]);
					if (t == null)
						break;
				}
				if (t != null) {
					fail = t;
					break;
				}
			}
			foreach((k, subTrie) -> {
				if (k != 0 && subTrie != this) {
					stack[i] = k;
					subTrie.calFail(root, stack, i + 1);
				}
			});
		}

		boolean contains(@NotNull String str) {
			Trie trie = this;
			for (int i = 0, n = str.length(); i < n; ) {
				Trie next = trie.get(str.charAt(i));
				if (next == null) {
					if (trie == this) {
						i++;
						continue;
					}
					trie = trie.fail;
					continue;
				}
				if (next == trie || next.hasZeroValue())
					return true;
				trie = next;
				i++;
			}
			return false;
		}

		boolean replace(char @NotNull [] chars, char replaceChar) {
			Trie trie = this;
			int iLast = 0, eLast = 0;
			boolean replaced = false;
			for (int i = 0, n = chars.length; i < n; ) {
				Trie next = trie.get(chars[i]);
				if (next == null) {
					if (iLast < eLast) {
						do
							chars[iLast++] = replaceChar;
						while (iLast < eLast);
						i = iLast;
						replaced = true;
						trie = this;
						continue;
					}
					if (trie == this) {
						i++;
						continue;
					}
					trie = trie.fail;
					continue;
				}
				if (next == trie) {
					for (int j = i - trie.deep; j <= i; j++)
						chars[j] = replaceChar;
					replaced = true;
					i++;
					continue;
				}
				if (next.hasZeroValue()) {
					trie = next;
					iLast = ++i - next.deep;
					eLast = i;
					continue;
				}
				trie = next;
				i++;
			}
			if (iLast < eLast) {
				do
					chars[iLast++] = replaceChar;
				while (iLast < eLast);
				replaced = true;
			}
			return replaced;
		}

		@Override
		public String toString() {
			return "Trie(" + size() + '/' + getKeyTable().length + ')';
		}
	}

	private @Nullable Trie root;
	private final HashSet<String> newAdds = new HashSet<>(); // 动态添加的部分
	private final FastLock newAddsLock = new FastLock();

	private static boolean addLine(@NotNull Trie trie, @NotNull String line) {
		line = line.trim();
		int e = line.length();
		if (e <= 0)
			return false;
		int i = (line.charAt(0) == 0xfeff) ? 1 : 0; // remove BOM
		if (i >= e)
			return false;
		trie.add(line, i, e);
		return true;
	}

	public int reload(@Nullable Reader reader) throws IOException {
		Trie trie = new Trie();
		int n = 0;
		if (reader != null) {
			BufferedReader br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null) {
				if (addLine(trie, line))
					n++;
			}
		}
		newAddsLock.lock();
		try {
			for (String line : newAdds) {
				if (addLine(trie, line))
					n++;
			}
		} finally {
			newAddsLock.unlock();
		}
		trie.calFail(trie, new char[1000], 0);
		root = trie;
		return n;
	}

	public int reload(@NotNull String filename, @Nullable Charset charset) throws IOException {
		if (charset == null)
			charset = StandardCharsets.UTF_8;
		try (Reader reader = new InputStreamReader(new FileInputStream(filename), charset)) {
			return reload(reader);
		}
	}

	public void addNewLine(@NotNull String line) { // 添加后需要reload才能生效
		newAddsLock.lock();
		try {
			newAdds.add(line);
		} finally {
			newAddsLock.unlock();
		}
	}

	public boolean contains(@NotNull String str) {
		return root != null && root.contains(str);
	}

	public @NotNull String replace(@NotNull String str, char replaceChar) {
		if (!contains(str))
			return str;
		char[] chars = str.toCharArray();
		//noinspection DataFlowIssue
		root.replace(chars, replaceChar);
		return new String(chars);
	}
}
