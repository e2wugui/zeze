package Zeze.Services.ServiceManager;

import java.util.Properties;
import Zeze.Util.PropertiesHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExporterConfig {
	// 已经实现的参数都写在这里，统一分配名字。
	// 分散的话，名字冲突不容易管理。
	// 这里还做了一些基本校验。更多校验可能在实现中。

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

	public String getReload() {
		return getString("-reload", null);
	}

	////////////////////////////////////////////////////////////////////////////
	// 下面是实现。
	private final Properties share;
	private final Properties privates;

	public ExporterConfig(@NotNull Properties share, @Nullable String privateParam) {
		this.share = share;
		this.privates = null == privateParam ? null : PropertiesHelper.parse(privateParam);

		//System.out.println("share: " + share);
		//System.out.println("private: " + privates);
	}

	public String getString(String name, String def) {
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
