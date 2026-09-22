package Zeze.Util;

/**
 * main 命令行参数解析的公共守卫。
 */
public final class Args {

	// 开关缺值时args[++i]抛无上下文的AIOOBE；这里给出明确的参数错误。
	public static String requireValue(String[] args, int index, String name) {
		if (index >= args.length)
			throw new IllegalArgumentException("argument '" + name + "' requires a value");
		return args[index];
	}

	public static int requireInt(String[] args, int index, String name) {
		return Integer.parseInt(requireValue(args, index, name));
	}

	public static boolean requireBool(String[] args, int index, String name) {
		return Boolean.parseBoolean(requireValue(args, index, name));
	}
}
