package Zeze.log;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import Zeze.Netty.HttpExchange;
import Zeze.Util.Json;
import Zeze.log.handle.entity.BaseResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * /api/* 处理器的浏览器源防御：ApiToken 未启用时（默认回环形态）"回环绑定=仅本机可达"
 * 的信任模型不覆盖受害者浏览器代发的请求——跨站简单请求（text/plain）可触发
 * search/browse 查询副作用；DNS rebinding（attacker.com→127.0.0.1）可越权读取全集群
 * 日志与 /api/query 透传。配置 Token 的部署不受影响（Authorization 头无法被简单请求伪造）。
 * 两道防线：Origin 非同源拒（浏览器跨站 POST 恒带 Origin 且不可伪造）；回环绑定下
 * Host 非回环字面量拒（rebinding 请求的 Host 是攻击者域名；纯文本比对，不对攻击者
 * 可控的主机名做 DNS 解析）。无 Origin 的非浏览器客户端（curl/脚本）不受影响；
 * Content-Type 不设限（预编译前端以无 body POST 调用，无该头）。
 * 进程内单例静态配置：由 {@link LogAgentManager#init} 从 ZokerManagerConf 装载。
 */
public final class BrowserOriginGuard {
	// 回环绑定形态（默认）：Host 防线启用。null=未装载——按默认绑定形态（回环）设防。
	private static volatile boolean loopbackBind = true;

	private BrowserOriginGuard() {
	}

	/** 装载绑定形态：回环绑定（含缺省/null/空白，对齐 ZokerManagerConf 默认回环）启用
	 * Host 回环防线；非回环绑定（必配 Token，checkDeployPolicy）下读取已有 Token 门。 */
	public static void configure(@Nullable String bind) {
		loopbackBind = bind == null || bind.isBlank() || ZokerManagerConf.isLoopback(bind.trim());
	}

	/** 处理器入口校验：通过返回 true；未通过时已回 403 并返回 false（调用方直接 return）。 */
	public static boolean check(@NotNull HttpExchange x) {
		var request = x.request();
		var headers = request != null ? request.headers() : null;
		var origin = headers != null ? headers.get(HttpHeaderNames.ORIGIN) : null;
		var host = headers != null ? headers.get(HttpHeaderNames.HOST) : null;
		if (isAllowed(origin, host, loopbackBind))
			return true;
		x.sendJson(HttpResponseStatus.FORBIDDEN,
				Json.toCompactString(BaseResponse.errorResult("forbidden request origin or host")));
		return false;
	}

	/**
	 * 纯判定（直测面）：Host 是同源比对与 rebinding 判定的基准，缺失（畸形请求）即拒；
	 * Origin 存在（浏览器代发）时必须与请求目标（Host）同源——跨站请求拒；
	 * 回环绑定下 Host 主机名必须是回环字面量——rebinding（Host=攻击者域名）拒。
	 */
	static boolean isAllowed(@Nullable String origin, @Nullable String host, boolean loopbackBind) {
		if (host == null || host.isBlank())
			return false;
		if (origin != null && !origin.isBlank()) {
			var originAuthority = normalizeAuthority(origin);
			if (originAuthority == null || !originAuthority.equals(normalizeAuthority(host)))
				return false;
		}
		if (loopbackBind)
			return isLoopbackHost(hostOf(host));
		return true;
	}

	/**
	 * 归一 authority 为 "host:port"（host 小写、端口显式；无端口的 http 默认 80、
	 * https 默认 443）——Origin（scheme://host[:port][/path]）与 Host（host[:port]）
	 * 统一形态后比对。不可解析（缺 host/端口非数字）返回 null（拒绝方向）。
	 */
	private static @Nullable String normalizeAuthority(String s) {
		var t = s.trim();
		var defaultPort = 80;
		var schemeEnd = t.indexOf("://");
		if (schemeEnd >= 0) {
			if (t.startsWith("https"))
				defaultPort = 443;
			t = t.substring(schemeEnd + 3);
		}
		var end = firstIndex(t, '/', '?', '#');
		if (end >= 0)
			t = t.substring(0, end);
		String host;
		var port = defaultPort;
		if (t.startsWith("[")) {
			var close = t.indexOf(']');
			if (close < 0)
				return null;
			host = t.substring(0, close + 1);
			var rest = t.substring(close + 1);
			if (!rest.isEmpty()) {
				if (!rest.startsWith(":"))
					return null;
				port = parsePort(rest.substring(1));
			}
		} else {
			var colon = t.lastIndexOf(':');
			if (colon >= 0) {
				host = t.substring(0, colon);
				port = parsePort(t.substring(colon + 1));
			} else
				host = t;
		}
		if (host.isEmpty() || port < 0)
			return null;
		return host.toLowerCase(Locale.ROOT) + ":" + port;
	}

	/** authority 的主机部分（去端口；[IPv6] 字面量保持括号便于回环字面量比对）。 */
	private static String hostOf(String authority) {
		var t = authority.trim();
		if (t.startsWith("[")) {
			var close = t.indexOf(']');
			return close < 0 ? t : t.substring(0, close + 1);
		}
		var colon = t.lastIndexOf(':');
		return colon < 0 ? t : t.substring(0, colon);
	}

	/**
	 * 回环主机判定（纯文本字面量，无 DNS）：localhost（大小写不敏感）、127.0.0.0/8 的
	 * IPv4 字面量、::1（含 [::1] 括号与 0:0:0:0:0:0:0:1 展开写法）。
	 */
	private static boolean isLoopbackHost(String host) {
		var h = host.trim();
		if (h.startsWith("["))
			h = h.endsWith("]") ? h.substring(1, h.length() - 1) : h.substring(1);
		if ("localhost".equalsIgnoreCase(h) || "::1".equals(h) || "0:0:0:0:0:0:0:1".equals(h))
			return true;
		var parts = h.split("\\.", -1);
		if (parts.length != 4 || !"127".equals(parts[0]))
			return false;
		for (var i = 1; i < 4; i++) {
			var seg = parts[i];
			if (seg.isEmpty() || seg.length() > 3)
				return false;
			for (var c : seg.toCharArray())
				if (c < '0' || c > '9')
					return false;
			if (Integer.parseInt(seg) > 255)
				return false;
		}
		return true;
	}

	private static int firstIndex(String s, char a, char b, char c) {
		var end = -1;
		for (var ch : new char[] {a, b, c}) {
			var i = s.indexOf(ch);
			if (i >= 0 && (end < 0 || i < end))
				end = i;
		}
		return end;
	}

	private static int parsePort(String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
