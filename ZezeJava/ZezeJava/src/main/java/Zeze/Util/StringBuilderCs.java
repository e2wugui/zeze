package Zeze.Util;

import org.jspecify.annotations.Nullable;

// C# 风格的 StringBuilder 便捷封装：appendLine/格式化 append 链式调用
public class StringBuilderCs {
	private final StringBuilder sb = new StringBuilder();

	public StringBuilderCs appendLine() {
		sb.append('\n');
		return this;
	}

	public StringBuilderCs appendLine(@Nullable String line) {
		sb.append(line).append('\n');
		return this;
	}

	public StringBuilderCs appendLine(String format, @Nullable Object... params) {
		sb.append(Str.format(format, params)).append('\n');
		return this;
	}

	public StringBuilderCs append(@Nullable String s) {
		sb.append(s);
		return this;
	}

	public StringBuilderCs append(String format, @Nullable Object... params) {
		sb.append(Str.format(format, params));
		return this;
	}

	public StringBuilderCs append(char c) {
		sb.append(c);
		return this;
	}

	public StringBuilderCs append(int i) {
		sb.append(i);
		return this;
	}

	public StringBuilderCs append(long l) {
		sb.append(l);
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
