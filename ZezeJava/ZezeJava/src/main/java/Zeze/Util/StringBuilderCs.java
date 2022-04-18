package Zeze.Util;

public class StringBuilderCs {
	private final StringBuilder sb = new StringBuilder();

	public StringBuilderCs AppendLine() {
		sb.append('\n');
		return this;
	}

	public StringBuilderCs AppendLine(String line) {
		sb.append(line).append('\n');
		return this;
	}

	public StringBuilderCs AppendLine(String format, Object... params) {
		sb.append(Str.format(format, params)).append('\n');
		return this;
	}

	public StringBuilderCs Append(String s) {
		sb.append(s);
		return this;
	}

	public StringBuilderCs Append(String format, Object... params) {
		sb.append(Str.format(format, params));
		return this;
	}

	public StringBuilderCs Append(char c) {
		sb.append(c);
		return this;
	}

	public StringBuilderCs Append(int i) {
		sb.append(i);
		return this;
	}

	public StringBuilderCs Append(long l) {
		sb.append(l);
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
