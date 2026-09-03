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
		private int maxWordLen; // 仅根节点使用：已添加的最大词条长度

		void add(@NotNull String str, int i, int e) {
			int len = e - i;
			if (len > maxWordLen) // add总是从根节点进入，在根上跟踪最大词条长度
				maxWordLen = len;
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
						// 贪心长词失败，替换积累的短词区间。不回退i、不回到根：
						// 沿fail保持积累词的后缀状态(此时尚未写入替换的chars[i]读取安全)，
						// 否则重叠词(前词后缀=后词前缀)的后缀起点被消费丢失。
						do
							chars[iLast++] = replaceChar;
						while (iLast < eLast);
						replaced = true;
						trie = trie.fail;
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
					// 叶子命中(父节点自引用)：词区间[i-trie.deep, i]。替换后需要恢复后缀状态，
					// 否则构成重叠词的已消费字符(如词表{ab,bc}的b)永远没有机会作为起点。
					// 此时chars[i]已被替换，用保存的原字符c沿fail链查找后缀转移(等效AC自动机
					// 虚拟叶子节点的fail)；后缀本身是完整词(叶自引用)时已被上面的替换区间覆盖，继续找更短后缀。
					char c = chars[i];
					Trie parent = trie; // 词尾字符的父节点，fail链从它的fail(最长后缀)开始枚举
					for (int j = i - trie.deep; j <= i; j++)
						chars[j] = replaceChar;
					replaced = true;
					trie = this;
					for (Trie t = parent.fail; ; t = t.fail) {
						Trie suffixNext = t.get(c);
						if (suffixNext != null && suffixNext != t) {
							trie = suffixNext;
							break;
						}
						if (t == t.fail) // root：后缀枚举完，回到根
							break;
					}
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

	// volatile: reload 锁外构建后一次性发布，保证读线程安全可见
	private volatile @Nullable Trie root;
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
		trie.calFail(trie, new char[Math.max(trie.maxWordLen, 1) + 1], 0); // 按最大词条长度分配calFail递归栈，避免固定栈越界
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
		var trie = root; // volatile字段先拷贝到局部，避免判空与使用间被并发修改
		return trie != null && trie.contains(str);
	}

	public @NotNull String replace(@NotNull String str, char replaceChar) {
		var trie = root; // 快照一次，让检查与替换落在同一代trie上
		if (trie == null || !trie.contains(str))
			return str;
		char[] chars = str.toCharArray();
		trie.replace(chars, replaceChar);
		return new String(chars);
	}
}
