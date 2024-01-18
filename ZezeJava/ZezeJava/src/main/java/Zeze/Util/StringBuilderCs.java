package Zeze.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringBuilderCs {
	private final StringBuilder sb = new StringBuilder();

	public @NotNull StringBuilderCs appendLine() {
		sb.append('\n');
		return this;
	}

	public @NotNull StringBuilderCs appendLine(@Nullable String line) {
		sb.append(line).append('\n');
		return this;
	}

	public @NotNull StringBuilderCs appendLine(@NotNull String format, @Nullable Object... params) {
		sb.append(Str.format(format, params)).append('\n');
		return this;
	}

	public @NotNull StringBuilderCs append(@Nullable String s) {
		sb.append(s);
		return this;
	}

	public @NotNull StringBuilderCs append(@NotNull String format, @Nullable Object... params) {
		sb.append(Str.format(format, params));
		return this;
	}

	public @NotNull StringBuilderCs append(char c) {
		sb.append(c);
		return this;
	}

	public @NotNull StringBuilderCs append(int i) {
		sb.append(i);
		return this;
	}

	public @NotNull StringBuilderCs append(long l) {
		sb.append(l);
		return this;
	}

	@Override
	public @NotNull String toString() {
		return sb.toString();
	}
}
