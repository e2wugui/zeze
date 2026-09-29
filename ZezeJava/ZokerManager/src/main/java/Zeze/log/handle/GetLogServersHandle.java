package Zeze.log.handle;

import java.util.ArrayList;
import java.util.Set;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.LogAgent;
import Zeze.Util.Json;
import Zeze.log.ApiToken;
import Zeze.log.BrowserOriginGuard;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * /api/get_log_servers 处理器：返回当前可用的日志服务器名列表。
 */
public class GetLogServersHandle implements HttpEndStreamHandle {
	@Override
	public void onEndStream(HttpExchange x) throws Exception {
		// token门（FND29 zokermanager-02）：配置了Token则校验Authorization头，未通过已回401。
		if (!ApiToken.check(x))
			return;
		// 浏览器源防御（FND31 zokermanager-03）：Origin 非同源（CSRF）或回环绑定下 Host
		// 非回环（DNS rebinding）已回403——服务器名列表也是 rebinding 读取目标。
		if (!BrowserOriginGuard.check(x))
			return;
		LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
		Set<String> logServers = logAgent.getLogServers();
		var baseResponse = BaseResponse.succResult(new ArrayList<>(logServers));
		x.sendJson(HttpResponseStatus.OK, Json.toCompactString(baseResponse));
	}
}
