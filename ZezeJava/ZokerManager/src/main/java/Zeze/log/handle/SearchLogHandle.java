package Zeze.log.handle;

import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.log.ApiToken;
import Zeze.log.BrowserOriginGuard;

/**
 * /api/search 处理器：按条件搜索日志（复用或重建查询会话）。
 * 执行流（校验/会话/分诊）与 /api/browse 同根同款，收口在 {@link SearchBrowse#serve}。
 */
public class SearchLogHandle implements HttpEndStreamHandle {
	@Override
	public void onEndStream(HttpExchange x) {
		// token门（FND29 zokermanager-02）：配置了Token则校验Authorization头，未通过已回401。
		if (!ApiToken.check(x))
			return;
		// 浏览器源防御（FND31 zokermanager-03）：Origin 非同源（CSRF）或回环绑定下 Host
		// 非回环（DNS rebinding）已回403——默认回环+无Token形态对浏览器代发请求设防。
		if (!BrowserOriginGuard.check(x))
			return;
		SearchBrowse.serve(x, false, "/api/search");
	}
}
