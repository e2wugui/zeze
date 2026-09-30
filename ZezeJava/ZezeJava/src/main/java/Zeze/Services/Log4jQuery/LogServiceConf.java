package Zeze.Services.Log4jQuery;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
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
			// 空白/纯点号（漏配时getAttribute返回""）在manager构造期才爆，异常无配置指向
			//（log4jquery-01）：纯点号串split("\\.")为空数组、取fulls[0]裸越界，空串则退化成
			// 对logDir目录本身开文件。配置错误在parse期fail-fast为指向字段的明确异常
			//（对齐Config侧"字段名 must ...: 值"惯例）。
			if (!isValidLogActive(logActive))
				throw new IllegalStateException("LogConf LogActive must be a non-blank file name and not '.'-only: <"
						+ logActive + "> (漏配LogActive？它是active日志文件名，如zeze.log)");
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
			// 三项格式配置parse期fail-fast（对齐上方LogActive先例）：非法值要到首次
			// 轮转/查询才以运行期异常暴露且形态不指向字段（非法LogTimeFormat在
			// Log4jFileSession构造内抛IAE泄漏fd；非法LogDatePattern使对账/轮转整轮
			// 被吞、rotate永不登记）。probe一次构造/查表（含默认值同过校验）。
			try {
				new SimpleDateFormat(logTimeFormat);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("LogConf LogTimeFormat must be a legal SimpleDateFormat pattern: <"
						+ logTimeFormat + "> (" + e.getMessage() + ")");
			}
			try {
				new SimpleDateFormat(logDatePattern);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("LogConf LogDatePattern must be a legal SimpleDateFormat pattern: <"
						+ logDatePattern + "> (" + e.getMessage() + ")");
			}
			if (!Charset.isSupported(charsetName))
				throw new IllegalStateException("LogConf CharsetName must be a supported charset: <" + charsetName + ">");
		}

		public String getName() {
			return logActive;
		}

		/** LogActive合法性判据（LogConf(Element)的parse校验与Log4jFileManager构造的防御校验共用）：
		 * 非null、非空白、按'.'分段非空。纯点号串（"."、".."）的split("\\.")为空数组——manager
		 * 构造对其取fulls[0]裸越界；空串则getCurrentLogFileName()为""、退化成对logDir目录本身
		 * 开文件。两类退化形态都在入口拒绝，不让配置错误以运行时越界/文件系统异常的形态逃逸。 */
		public static boolean isValidLogActive(String logActive) {
			return logActive != null && !logActive.isBlank() && logActive.split("\\.").length > 0;
		}
	}

	public String serviceIdentity = "#LogService_{serverId}_{host}_{port}";
	// 查询会话空闲过期阈值（毫秒）：NewSession/查询路径顺带清理超龄会话；<=0禁用。默认1小时（小时级，
	// 需显著大于正常翻页间隔——回收正在翻页的会话会打断查询）。
	public long sessionIdleTimeoutMillis = 3_600_000;
	private final ConcurrentHashMap<String, LogConf> logConfs = new ConcurrentHashMap<>();
	// 主 LogConf 名（配置顺序首个）：多日志部署的缺省查询目标。map 无序（CHM 不保
	// 插入序），配置序单独记忆——首个 LogConf 是主日志，无日志名参数的查询前端以它
	// 为缺省解析（见 SearchLogParam.resolveLogName）。仅 parse 填写；无 LogConf 时为 null。
	private String primaryLogName;

	@Override
	public @NotNull String getName() {
		return "LogServiceConf";
	}

	public ConcurrentHashMap<String, LogConf> getLogConfs() {
		return logConfs;
	}

	/** 主 LogConf 名（配置顺序首个，见字段注释）；零 LogConf 或仅程序化填 map 的形态为 null。 */
	public String getPrimaryLogName() {
		return primaryLogName;
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
			if (primaryLogName == null)
				primaryLogName = logConf.getName(); // 首个（配置顺序）即主日志，见字段注释
		}
	}
}
