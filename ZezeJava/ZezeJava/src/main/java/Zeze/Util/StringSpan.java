package Zeze.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 字符串零拷贝视图（string + offset + length），避免substring分配。
 *
 * 跨类型互查契约（FND8-14）：equals/compareTo与String的单向互查是蓄意设计——
 * 本类实例可作String键容器（HashMap&lt;String,String&gt;等）的查询参数（hashCode与
 * String保持一致，get/contains走本类equals命中String键）；但String.equals不认本类，
 * 反向不成立：禁止将本类实例作为哈希容器key存入后再以等值String查询/删除——
 * 哈希桶命中而equals静默miss。compareTo对异类恒返-1，同为单向。
 */
public class StringSpan implements Comparable<Object> {
	private @NotNull String string;
	private int offset;
	private int length;

	public StringSpan(@NotNull String string) {
		this(string, 0, string.length());
	}

	public StringSpan(@NotNull String string, int length) {
		this(string, 0, length);
	}

	public StringSpan(@NotNull String string, int offset, int length) {
		this.string = string;
		this.offset = offset;
		this.length = length;
	}

	public @NotNull String getString() {
		return string;
	}

	public void setString(@NotNull String string) {
		this.string = string;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int i = offset, e = i + length; i < e; i++)
			h = 31 * h + string.charAt(i); // 必须跟String的实现保持一致
		return h;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof String s1) {
			var s0 = string;
			var n0 = length;
			var o0 = offset;
			if (n0 != s1.length())
				return false;
			for (int i = 0; i < n0; i++) {
				if (s0.charAt(o0 + i) != s1.charAt(i))
					return false;
			}
			return true;
		}
		if (obj instanceof StringSpan ss) {
			var s0 = string;
			var s1 = ss.string;
			var n0 = length;
			var o0 = offset;
			var o1 = ss.offset;
			if (n0 != ss.length)
				return false;
			for (int i = 0; i < n0; i++) {
				if (s0.charAt(o0 + i) != s1.charAt(o1 + i))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(@Nullable Object obj) {
		if (this == obj)
			return 0;
		if (obj instanceof String s1) {
			var s0 = string;
			var n0 = length;
			var n1 = s1.length();
			var o0 = offset;
			int n = Math.min(n0, n1);
			for (int i = 0; i < n; i++) {
				int a = s0.charAt(o0 + i);
				int b = s1.charAt(i);
				if (a != b)
					return a - b;
			}
			return n0 - n1;
		}
		if (obj instanceof StringSpan ss) {
			var s0 = string;
			var s1 = ss.string;
			var n0 = length;
			var n1 = ss.length;
			var o0 = offset;
			var o1 = ss.offset;
			int n = Math.min(n0, n1);
			for (int i = 0; i < n; i++) {
				int a = s0.charAt(o0 + i);
				int b = s1.charAt(o1 + i);
				if (a != b)
					return a - b;
			}
			return n0 - n1;
		}
		return -1;
	}

	@Override
	public @NotNull String toString() {
		return string.substring(offset, offset + length);
	}
}
