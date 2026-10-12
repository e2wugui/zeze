package Zeze.log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * ZokerManager HTTP 管理口部署契约（FND29 zokermanager-02）：Bind 默认回环、Token 可选。
 * 解析 server.xml 的 {@code <CustomizeConf Name="ZokerManagerConf" .../>}，缺省时全部取字段默认值。
 */
public class ZokerManagerConf implements Config.ICustomize {
	// 迁移：旧版本 httpServer.start(netty, 9980) 未指定 host，Netty 绑 0.0.0.0（全部网卡）且
	// 无任何认证。新默认回环（对齐本模块 LogService Acceptor Ip="127.0.0.1" 的既有姿态），
	// 远程访问必须显式 Bind="0.0.0.0"/指定网卡IP 且配 Token（见 checkDeployPolicy）。
	public static final String DEFAULT_BIND = "127.0.0.1";

	public @NotNull String bind = DEFAULT_BIND;
	// 空=不校验；仅回环默认部署无需配置。非回环部署为空时启动 fail-fast（checkDeployPolicy）。
	public @NotNull String token = "";

	@Override
	public @NotNull String getName() {
		return "ZokerManagerConf";
	}

	@Override
	public void parse(@NotNull Element self) {
		// 属性空白（漏配/显式置空）保持默认回环，不回退旧全网卡语义——opt-in 只认显式 "0.0.0.0"/具体地址。
		var attr = self.getAttribute("Bind");
		if (!attr.isBlank())
			bind = attr.trim();
		attr = self.getAttribute("Token");
		if (!attr.isBlank())
			token = attr.trim();
	}

	/** 回环判定（InetAddress 语义：127/8、::1 及其书写变体、localhost）。解析失败按非回环处理（fail-closed）。 */
	public static boolean isLoopback(@NotNull String bind) {
		try {
			return InetAddress.getByName(bind).isLoopbackAddress();
		} catch (UnknownHostException e) {
			return false;
		}
	}

	/**
	 * 启动期部署契约校验（FND29 zokermanager-02）：绑非回环地址且未配 Token 时 fail-fast——
	 * 该组合等于把全集群日志读取/透传查询面无认证暴露给网络，不允许默认放行。
	 */
	public static void checkDeployPolicy(@NotNull String bind, @NotNull String token) {
		if (!isLoopback(bind) && token.isBlank())
			throw new IllegalStateException("ZokerManagerConf: HTTP admin port bound to non-loopback "
					+ bind + " without Token - refusing to start (unauthenticated cluster log access, "
					+ "FND29 zokermanager-02). Either keep Bind local (default 127.0.0.1), or configure "
					+ "<CustomizeConf Name=\"ZokerManagerConf\" Bind=\"" + bind + "\" Token=\"<secret>\"/> "
					+ "in server.xml and send the token in the Authorization header of every /api/* request.");
	}
}
