package Zeze.Util;

import java.util.Objects;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 字符串匹配大量固定子串的高效算法. 使用trie树结构搭配AC自动机算法, 内存占用大概是所有子串的30倍大小
 */
public final class StringChecker {
	private static final Logger logger = LogManager.getLogger(StringChecker.class);

	/** 词条长度上限，防止 calFail 深递归导致栈溢出。 */
	private static final int MAX_WORD_LENGTH = 1024;
	private static final class Trie extends CharHashMap<Trie> {
		private int deep;
		private @Nullable Trie fail;
		private boolean end; // 词尾标志
		private int maxWordLen; // 仅根节点使用：已添加的最大词条长度

		void add(String str, int i, int e) {
			int len = e - i;
			if (len > maxWordLen)
				maxWordLen = len;
			int start = i; // 记录起点以计算相对深度，避免 BOM 首行导致替换区间偏移
			for (Trie trie = this, next; ; trie = next) {
				trie.deep = i - start;
				char c = str.charAt(i);
				next = trie.get(c);
				if (++i >= e) {
					// 词尾独立成节点并置 end，保证 fail 链可命中真后缀词条
					if (next == null)
						trie.put(c, next = new Trie());
					next.deep = i - start; // 词尾深度=词长，供 replace 计算区间
					next.end = true;
					return;
				}
				if (next == null)
					trie.put(c, next = new Trie());
			}
		}

		void calFail(Trie root, char [] stack, int i) {
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

		boolean contains(String str) {
			Trie trie = this;
			for (int i = 0, n = str.length(); i < n; ) {
				Trie next = trie.get(str.charAt(i));
				if (next == null) {
					if (trie == this) {
						i++;
						continue;
					}
					trie = Objects.requireNonNull(trie.fail);
					continue;
				}
				if (next.end)
					return true;
				// 沿 fail 链检查后缀输出，避免漏检作为长词真后缀的短词
				for (Trie t = Objects.requireNonNull(next.fail); ; t = Objects.requireNonNull(t.fail)) {
					if (t.end)
						return true;
					if (t == t.fail) // 到达根节点
						break;
				}
				trie = next;
				i++;
			}
			return false;
		}

		@SuppressWarnings("UnusedReturnValue")
		boolean replace(char [] chars, char replaceChar) {
			Trie trie = this;
			int iLast = 0, eLast = 0;
			boolean replaced = false;
			for (int i = 0, n = chars.length; i < n; ) {
				Trie next = trie.get(chars[i]);
				if (next == null) {
					if (iLast < eLast) {
						// 贪心长词失配：替换挂起短词后沿 fail 保留后缀状态，不丢失重叠词起点
						do
							chars[iLast++] = replaceChar;
						while (iLast < eLast);
						replaced = true;
						trie = Objects.requireNonNull(trie.fail);
						continue;
					}
					if (trie == this) {
						i++;
						continue;
					}
					trie = Objects.requireNonNull(trie.fail);
					continue;
				}
				if (next.end && next.isEmpty()) {
					// 纯词尾命中：合并/刷新挂起区间并替换当前词，沿 next.fail 保留重叠后缀状态
					int start = i + 1 - next.deep;
					if (iLast < eLast) {
						if (start >= eLast) {
							do
								chars[iLast++] = replaceChar;
							while (iLast < eLast);
						} else if (iLast < start) {
							start = iLast;
						}
						iLast = eLast = 0;
					}
					for (int j = start; j <= i; j++)
						chars[j] = replaceChar;
					replaced = true;
					trie = Objects.requireNonNull(next.fail);
					i++;
					continue;
				}
				// 非纯词尾：取自身或 fail 链上命中的最长词做贪心积累
				int wordLen = 0;
				if (next.end)
					wordLen = next.deep;
				else {
					for (Trie t = Objects.requireNonNull(next.fail); ; t = Objects.requireNonNull(t.fail)) {
						if (t.end) {
							wordLen = t.deep;
							break;
						}
						if (t == t.fail)
							break;
					}
				}
				trie = next;
				++i;
				if (wordLen > 0) {
					// 与挂起区间不重叠则先刷新旧区间，重叠则合并保留更早左边界
					int start = i - wordLen;
					if (iLast < eLast) {
						if (start >= eLast) {
							do
								chars[iLast++] = replaceChar;
							while (iLast < eLast);
							replaced = true;
							iLast = start;
						} else if (start < iLast) {
							iLast = start;
						}
					} else {
						iLast = start;
					}
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

	private static boolean addLine(Trie trie, String line) {
		line = line.trim();
		int e = line.length();
		if (e == 0)
			return false;
		if (e > MAX_WORD_LENGTH) { // 拒绝超长词条，防calFail深递归SOE
			logger.warn("StringChecker: word too long ({} > {}), rejected: '{}'...",
					e, MAX_WORD_LENGTH, line.substring(0, 32));
			return false;
		}
		int i = (line.charAt(0) == 0xfeff) ? 1 : 0; // 跳过 UTF-8 BOM
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

	public int reload(String filename, @Nullable Charset charset) throws IOException {
		if (charset == null)
			charset = StandardCharsets.UTF_8;
		try (Reader reader = new InputStreamReader(new FileInputStream(filename), charset)) {
			return reload(reader);
		}
	}

	public void addNewLine(String line) { // 添加后需要reload才能生效
		newAddsLock.lock();
		try {
			newAdds.add(line);
		} finally {
			newAddsLock.unlock();
		}
	}

	public boolean contains(String str) {
		var trie = root; // volatile字段先拷贝到局部，避免判空与使用间被并发修改
		return trie != null && trie.contains(str);
	}

	public String replace(String str, char replaceChar) {
		var trie = root; // 快照一次，让检查与替换落在同一代trie上
		if (trie == null || !trie.contains(str))
			return str;
		char[] chars = str.toCharArray();
		trie.replace(chars, replaceChar);
		return new String(chars);
	}
}
