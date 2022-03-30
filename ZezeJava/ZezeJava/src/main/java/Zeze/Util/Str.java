package Zeze.Util;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;

public final class Str {
	public static final ParameterizedMessageFactory Formatter = new ParameterizedMessageFactory();
	public static final int INDENT_MAX = 64;
	private static final String[] INDENTS = new String[INDENT_MAX];

	private Str() {
	}

	static {
		for (int i = 0; i < INDENT_MAX; i++)
			INDENTS[i] = " ".repeat(i);
	}

	public static String format(String f, Object... params) {
		return Formatter.newMessage(f, params).getFormattedMessage();
	}

	public static String indent(int n) {
		if (n <= 0)
			return "";
		if (n >= INDENT_MAX)
			n = INDENT_MAX - 1;
		return INDENTS[n];
	}
}
