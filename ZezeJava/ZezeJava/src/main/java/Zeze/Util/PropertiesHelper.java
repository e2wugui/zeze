package Zeze.Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class PropertiesHelper {
	private static final Logger logger = LogManager.getLogger(PropertiesHelper.class);

	private PropertiesHelper() {
	}

	/**
	 * props 格式为： -key [value] -key2 [value] ...
	 * 1. -key不能重名，重复时，保留第一个，其他丢弃。
	 * 2. value 不存在时，设为""写入Properties。
	 * 3. 只支持按空格简单分割，不支持双引号括起来的值。
	 *
	 * @param props string
	 * @return Properties
	 */
	public static @NotNull Properties parse(@NotNull String props) {
		var args = props.split(" ");
		//System.out.println(Arrays.toString(args));

		var result = new Properties();
		for (var i = 0; i < args.length; ++i) {
			if (args[i].startsWith("-")) {
				var key = args[i]; // eat key
				String value = ""; // default for no value
				if (i + 1 < args.length && !args[i + 1].startsWith("-") /* peek next if is value */)
					value = args[++i]; // eat value
				result.putIfAbsent(key, value);
				continue;
			}
			throw new IllegalArgumentException("value found without -key=" + args[i] + " props=" + props);
		}
		return result;
	}

	public static int loadProperties(@NotNull String fileName) throws IOException {
		var file = new File(fileName);
		if (!file.isFile())
			return -1;
		var props = new Properties();
		try (var fr = new FileReader(file, StandardCharsets.UTF_8)) {
			props.load(fr);
		}
		System.getProperties().putAll(props);
		return props.size();
	}

	public static boolean getBool(@NotNull String key, boolean def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		try {
			return Boolean.parseBoolean(value);
		} catch (NumberFormatException e) {
			logger.warn("invalid bool property '{}' = '{}'", key, value);
			return def;
		}
	}

	public static int getInt(@NotNull String key, int def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.warn("invalid int property '{}' = '{}'", key, value);
			return def;
		}
	}

	public static long getLong(@NotNull String key, long def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			logger.warn("invalid long property '{}' = '{}'", key, value);
			return def;
		}
	}

	public static float getFloat(@NotNull String key, float def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			logger.warn("invalid float property '{}' = '{}'", key, value);
			return def;
		}
	}

	public static double getDouble(@NotNull String key, double def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			logger.warn("invalid double property '{}' = '{}'", key, value);
			return def;
		}
	}

	public static String getString(@NotNull String key, String def) {
		var value = System.getProperty(key);
		return value != null ? value : def;
	}
}
