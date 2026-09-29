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
			String result = logAgent.query(serverName, queryParam.getJson());
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(result)));
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
