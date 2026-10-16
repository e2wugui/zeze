package Zeze.Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

// 配置读取辅助：类型化 getProperty 与 "-key [value]" 串解析
public final class PropertiesHelper {
	private static final Logger logger = LogManager.getLogger(PropertiesHelper.class);
	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");

	private PropertiesHelper() {
	}

	@Contract("_, _, !null -> !null")
	public static @Nullable String getString(Map<String, String> anyProps, String key, @Nullable String def) {
		var value = anyProps.get(key);
		return value != null ? value : def;
	}

	public static int getInt(Map<String, String> anyProps, String key, int def) {
		var value = anyProps.get(key);
		return value != null ? Integer.parseInt(value) : def;
	}

	/**
	 * props 格式为： -key [value] -key2 [value] ...
	 * 1. -key不能重名，重复时，保留第一个，其他丢弃。
	 * 2. value 不存在时，设为""写入Properties。
	 * 3. 只支持按空白简单分割，不支持双引号括起来的值。
	 */
	public static Properties parse(String props) {
		// trim+\\s+：容忍连续/首尾空白。split(" ") 会把连续空格产生的空 token 当作“无 -key 的 value”误报。
		var args = PATTERN_SPACES.split(props.trim());

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

	public static int loadProperties(String fileName) throws IOException {
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

	public static boolean getBool(String key, boolean def) {
		var value = System.getProperty(key);
		if (value == null || (value = value.trim()).isEmpty())
			return def;
		// Boolean.parseBoolean 只识别忽略大小写的"true"、其余一律返回false且从不抛异常，
		// 不能用try-catch(NumberFormatException)——那是死代码，垃圾值(-Dx=yes)会被静默当成false。
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;
		logger.warn("invalid bool property '{}' = '{}'", key, value);
		return def;
	}

	public static int getInt(String key, int def) {
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

	public static long getLong(String key, long def) {
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

	public static float getFloat(String key, float def) {
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

	public static double getDouble(String key, double def) {
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

	@Contract("_, !null -> !null")
	public static @Nullable String getString(String key, @Nullable String def) {
		var value = System.getProperty(key);
		return value != null ? value : def;
	}
}
