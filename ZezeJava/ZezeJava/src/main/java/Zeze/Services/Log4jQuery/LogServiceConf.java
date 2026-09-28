package Zeze.Services.Log4jQuery;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * LogService 配置：解析 xml 中的服务标识、查询会话空闲阈值与多份 LogConf（每份日志一套文件名与格式）。
 */
public class LogServiceConf implements Config.ICustomize {
	public static class LogConf {
		public String logActive;
		// 同一（logDir, logActive）同一时刻只允许一个Log4jFileManager管理（同一日志文件双管）：
		// indexLinks编号命名空间与<active>.index交接名按（目录,活性）定位且实例间（及跨进程间）
		// 无互斥，双管并发会撞号交错写/互删链接——进程内由Log4jFileManager构造期独占登记拒绝
		//（见其logDirOwners注释），跨进程属部署约束。同一logDir不同logActive（多份日志同目录）
		// 是合法形态：indexLinks按logActive分子目录互不共享。本字段parse后仍可改写，独占检查以
		// 构造时的实际值为准（parse期不做重复目录校验）。
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
	// 查询会话空闲过期阈值（毫秒）：NewSession/查询路径顺带清理超龄会话；<=0禁用。默认1小时（小时级，
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
