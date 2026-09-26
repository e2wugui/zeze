package Zeze.Services.Log4jQuery;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LogServiceConf implements Config.ICustomize {
	public static class LogConf {
		public String logActive;
		public String logDir = "log";
		public String logDatePattern = ".yyyy-MM-dd";
		public String logTimeFormat = "yy-MM-dd HH:mm:ss.SSS";
		public String charsetName = "utf-8";

		public LogConf() {
		}

		public LogConf(@NotNull Element self) {
			logActive = self.getAttribute("LogActive");
			var attr = self.getAttribute("LogDir");
			if (!attr.isBlank())
				logDir = attr;
			attr = self.getAttribute("LogDatePattern");
			if (!attr.isBlank())
				logDatePattern = attr;
			logTimeFormat = self.getAttribute("LogTimeFormat");
			if (logTimeFormat.isBlank())
				logTimeFormat = "yy-MM-dd HH:mm:ss.SSS";
			attr = self.getAttribute("CharsetName");
			if (!attr.isBlank())
				charsetName = attr;
		}

		public String getName() {
			return logActive;
		}
	}

	public String serviceIdentity = "#LogService_{serverId}_{host}_{port}";
	// 查询会话空闲过期阈值（毫秒，GD-D03）：NewSession/查询路径顺带清理超龄会话；<=0禁用。默认1小时（小时级，
	// 需显著大于正常翻页间隔——回收正在翻页的会话会打断查询）。
	public long sessionIdleTimeoutMillis = 3_600_000;
	private final ConcurrentHashMap<String, LogConf> logConfs = new ConcurrentHashMap<>();

	@Override
	public @NotNull String getName() {
		return "LogServiceConf";
	}

	public ConcurrentHashMap<String, LogConf> getLogConfs() {
		return logConfs;
	}

	public void formatServiceIdentity(int serverId, String host, int port) {
		var params = new HashMap<String, Object>();
		params.put("serverId", serverId);
		params.put("host", host);
		params.put("port", port);
		serviceIdentity = Str.format(serviceIdentity, params);
	}

	@Override
	public void parse(@NotNull Element self) {
		var attr = self.getAttribute("ServiceIdentity");
		if (!attr.isBlank())
			serviceIdentity = attr;
		attr = self.getAttribute("SessionIdleTimeoutMillis");
		if (!attr.isBlank())
			sessionIdleTimeoutMillis = Long.parseLong(attr);

		var childNodes = self.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (!node.getNodeName().equals("LogConf"))
				continue;

			Element e = (Element)node;
			var logConf = new LogConf(e);
			if (logConfs.putIfAbsent(logConf.getName(), logConf) != null)
				throw new RuntimeException("duplicate log conf name. " + logConf.getName());
		}
	}
}
