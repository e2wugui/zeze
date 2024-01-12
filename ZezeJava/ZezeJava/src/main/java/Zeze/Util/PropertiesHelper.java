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
