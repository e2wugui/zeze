package Zeze.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import Zeze.Netty.HttpExchange;
import Zeze.Util.Json;
import Zeze.log.handle.entity.BaseResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * HTTP 管理口 token 校验（FND29 zokermanager-02）：配置了 Token 则四个 /api/* 处理器入口
 * 校验 {@code Authorization} 请求头（原值精确等于配置值）；未配置则全放行（默认回环部署形态）。
 * 进程内单例静态配置：由 {@link LogAgentManager#init} 从 ZokerManagerConf 装载一次。
 */
public final class ApiToken {
	// null=未启用（Token 未配置）。byte[] 形态供常量时间比较。
	private static volatile byte @Nullable [] expected;

	private ApiToken() {
	}

	/** null/空白视为未启用；其余按 UTF-8 字节比较。 */
	public static void configure(@Nullable String token) {
		expected = token == null || token.isBlank() ? null : token.getBytes(StandardCharsets.UTF_8);
	}

	public static boolean isEnabled() {
		return expected != null;
	}

	/** 纯比较（不触 HTTP 层，可直测）。未启用时恒 false（放行语义由 check 承载）。 */
	public static boolean matches(@Nullable String presented) {
		var e = expected;
		if (e == null || presented == null)
			return false;
		// 常量时间比较（防时序侧信道逐字节泄露）；长度不等时也走完比较流程。
		return MessageDigest.isEqual(e, presented.getBytes(StandardCharsets.UTF_8));
	}

	/** 处理器入口校验：通过返回 true；未通过时已回 401 并返回 false（调用方直接 return）。 */
	public static boolean check(@NotNull HttpExchange x) {
		if (!isEnabled())
			return true;
		var request = x.request();
		var presented = request != null ? request.headers().get(HttpHeaderNames.AUTHORIZATION) : null;
		if (matches(presented))
			return true;
		x.sendJson(HttpResponseStatus.UNAUTHORIZED, Json.toCompactString(BaseResponse.errorResult("unauthorized")));
		return false;
	}
}
