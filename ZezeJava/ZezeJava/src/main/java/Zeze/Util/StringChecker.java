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
		private boolean end; // 词尾：root到此节点的路径是一个完整词条（终止状态显式化）
		private int maxWordLen; // 仅根节点使用：已添加的最大词条长度

		void add(@NotNull String str, int i, int e) {
			int len = e - i;
			if (len > maxWordLen) // add总是从根节点进入，在根上跟踪最大词条长度
				maxWordLen = len;
			int start = i; // 进入时的快照：BOM行start=1。deep必须记录相对深度(i-start)，
			// 若记录绝对下标i，BOM首行词条的replace区间[i-deep,i]会左扩1个字符(多censor一个无辜前导字符)。
			for (Trie trie = this, next; ; trie = next) {
				trie.deep = i - start;
				char c = str.charAt(i);
				next = trie.get(c);
				if (++i >= e) {
					// 词尾统一为真实节点+end标志（FND4-09）：曾经的自引用叶(标记在父的边上)
					// 让词尾不占状态，fail链上的输出检查在语义上无法成立——词条是另一词条
					// 真后缀时，命中信号经由长词前缀路径的fail链到达，只查转移目标必然漏检。
					if (next == null)
						trie.put(c, next = new Trie());
					next.deep = i - start; // 词尾节点深度=词长（replace区间计算依赖）
					next.end = true;
					return;
				}
				if (next == null)
					trie.put(c, next = new Trie());
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
				stack[i] = k;
				subTrie.calFail(root, stack, i + 1);
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
				if (next.end)
					return true;
				// 成功转移后沿fail链检查输出（FND4-09）：词条是另一词条真后缀时，
				// 命中信号经由长词前缀路径的fail链到达，只查转移目标自身会漏检。
				for (Trie t = next.fail; ; t = t.fail) {
					if (t.end)
						return true;
					if (t == t.fail) // root：后缀枚举完
						break;
				}
				trie = next;
				i++;
			}
			return false;
		}

		@SuppressWarnings("UnusedReturnValue")
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
				if (next.end && next.isEmpty()) {
					// 纯词尾命中(词条无更长扩展)：词区间[i+1-deep, i]立即替换。替换后需要
					// 恢复后缀状态，否则构成重叠词的已消费字符(如词表{ab,bc}的b)永远没有机会
					// 作为起点：命中词W的最长后缀状态next.fail继续匹配（其字符虽已被替换，
					// 状态机只依赖状态不依赖字符）；后缀本身是完整词时已被替换区间覆盖。
					for (int j = i + 1 - next.deep; j <= i; j++)
						chars[j] = replaceChar;
					replaced = true;
					trie = next.fail;
					i++;
					continue;
				}
				// 非纯词尾转移：计算当前位置命中的最长词（贪心积累，失配或更长词命中时消费）。
				// 词尾在自身(next.end，非叶形态：还是更长词条的前缀)或在fail链上——
				// fail链命中即真后缀词条场景（FND4-09）：取链上首个end（最长后缀词）。
				int wordLen = 0;
				if (next.end)
					wordLen = next.deep;
				else {
					for (Trie t = next.fail; ; t = t.fail) {
						if (t.end) {
							wordLen = t.deep;
							break;
						}
						if (t == t.fail) // root
							break;
					}
				}
				trie = next;
				++i;
				if (wordLen > 0) {
					iLast = i - wordLen;
					eLast = i;
				}
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
		if (e == 0)
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
