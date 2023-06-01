package Zeze.Util;

public class PropertiesHelper {
	public static int getInt(String name, int def) {
		var p = System.getProperty(name);
		if (null == p || p.isBlank())
			return def;
		return Integer.parseInt(p);
	}

	public static long getLong(String name, long def) {
		var p = System.getProperty(name);
		if (null == p || p.isBlank())
			return def;
		return Long.parseLong(p);
	}

	public static String getString(String name, String def) {
		var p = System.getProperty(name);
		if (null == p || p.isBlank())
			return def;
		return p.trim();
	}
}
