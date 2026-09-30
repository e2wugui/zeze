package Zeze.log.handle;

import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.LogAgent;
import Zeze.Util.Json;
import Zeze.log.ApiToken;
import Zeze.log.BrowserOriginGuard;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import Zeze.log.handle.entity.QueryParam;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * /api/query 处理器：把查询请求转发给指定日志服务器并回传结果。
 */
public class QueryHandle implements HttpEndStreamHandle {
	@Override
	public void onEndStream(HttpExchange x) throws Exception {
		// token门（FND29 zokermanager-02）：配置了Token则校验Authorization头，未通过已回401。
		if (!ApiToken.check(x))
			return;
		// 浏览器源防御（FND31 zokermanager-03）：Origin 非同源（CSRF）或回环绑定下 Host
		// 非回环（DNS rebinding）已回403——query 透传全集群查询面，同受防线覆盖。
		if (!BrowserOriginGuard.check(x))
			return;
		try {
			ByteBuf content = x.content();
			int readableBytes = content.readableBytes();
			byte[] bytes = new byte[readableBytes];
			content.readBytes(bytes);
			String str = new String(bytes, StandardCharsets.UTF_8);
			QueryParam queryParam = Json.parse(str, QueryParam.class);
			LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
			String serverName = queryParam.getServerName();
			// serverName 入口预校验（对称 search/browse 的注册表比对）：缺字段/空白/未注册名
			// 以明确 errorResult 分诊，不透传代理（null 即 NPE、未注册名 IAE 同坍缩 system error）。
			if (serverName == null || serverName.trim().isEmpty()) {
				x.sendJson(HttpResponseStatus.OK,
						Json.toCompactString(BaseResponse.errorResult("missing serverName")));
				return;
			}
			serverName = serverName.trim();
			if (!logAgent.getLogServers().contains(serverName)) {
				x.sendJson(HttpResponseStatus.OK,
						Json.toCompactString(BaseResponse.errorResult("unknown log server: " + serverName)));
				return;
			}
			// json 入口预校验（与 serverName 同款分诊纪律）：缺失（JSON缺字段→null）透传
			// 到 BJson 编码对 null 调 isEmpty() 抛 NPE；空白串按空序列化省略，服务端
			// Json.parse("") 越界抛异常回错误码——两者同坍缩 system error（空串形态还
			// 放大成两端日志噪音）。入口即拒：明确 errorResult，不进代理透传。
			String json = queryParam.getJson();
			if (json == null || json.isBlank()) {
				x.sendJson(HttpResponseStatus.OK,
						Json.toCompactString(BaseResponse.errorResult("missing json")));
				return;
			}
			String result = logAgent.query(serverName, json);
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(result)));
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
