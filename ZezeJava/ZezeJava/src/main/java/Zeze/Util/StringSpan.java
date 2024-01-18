package Zeze.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		if (obj instanceof String) {
			var s0 = string;
			var s1 = (String)obj;
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
		if (obj instanceof StringSpan) {
			var ss = (StringSpan)obj;
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
		if (obj instanceof String) {
			var s0 = string;
			var s1 = (String)obj;
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
		if (obj instanceof StringSpan) {
			var ss = (StringSpan)obj;
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
