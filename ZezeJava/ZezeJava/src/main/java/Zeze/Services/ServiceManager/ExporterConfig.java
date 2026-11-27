package Zeze.Services.ServiceManager;

import java.util.Properties;
import Zeze.Util.PropertiesHelper;
import org.jspecify.annotations.Nullable;

/**
 * 导出器参数配置：共享Properties叠加-private私有参数，统一参数名分配与读取。
 */
public class ExporterConfig {
	/** 读取 -version 选桶版本，默认 0，仅导出与注册 version 一致的服务桶。 */
	public long getVersion() {
		return getLong("-version", 0L);
	}

	public String getFile() {
		var p = getString("-file", null);
		if (null == p)
			throw new IllegalArgumentException("-file not exist.");
		return p;
	}

	public String getUrl() {
		var p = getString("-url", null);
		if (null == p)
			throw new IllegalArgumentException("-url not exist.");
		return p;
	}

	public @Nullable String getReload() {
		return getString("-reload", null);
	}

	private final Properties share;
	private final @Nullable Properties privates;

	public ExporterConfig(Properties share, @Nullable String privateParam) {
		this.share = share;
		this.privates = null == privateParam ? null : PropertiesHelper.parse(privateParam);
	}

	public @Nullable String getString(String name, @Nullable String def) {
		String p;
		if (null != privates && (p = privates.getProperty(name)) != null)
			return p;
		if ((p = share.getProperty(name)) != null)
			return p;
		return def;
	}

	public long getLong(String name, long def) {
		var p = getString(name, null);
		return null == p ? def : Long.parseLong(p);
	}

	public int getInt(String name, int def) {
		var p = getString(name, null);
		return null == p ? def : Integer.parseInt(p);
	}

	public boolean getBool(String name, boolean def) {
		var p = getString(name, null);
		return null == p ? def : Boolean.parseBoolean(p);
	}
}
