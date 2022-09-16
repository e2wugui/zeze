package Zeze.Util;

public class StringBuilderCs {
	private final StringBuilder sb = new StringBuilder();

	public StringBuilderCs appendLine() {
		sb.append('\n');
		return this;
	}

	public StringBuilderCs appendLine(String line) {
		sb.append(line).append('\n');
		return this;
	}

	public StringBuilderCs appendLine(String format, Object... params) {
		sb.append(Str.format(format, params)).append('\n');
		return this;
	}

	public StringBuilderCs append(String s) {
		sb.append(s);
		return this;
	}

	public StringBuilderCs append(String format, Object... params) {
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
