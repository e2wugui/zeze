package Zeze.log.handle;

import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.log.ApiToken;
import Zeze.log.BrowserOriginGuard;

/**
 * /api/browse 处理器：按条件过滤翻页浏览日志（复用或重建查询会话）。
 * 执行流（校验/会话/分诊）与 /api/search 同根同款，收口在 {@link SearchBrowse#serve}
 * （browse 语义位：offsetFactor 校验/指纹、browse 会话操作）。
 */
public class BrowseLogHandle implements HttpEndStreamHandle {
	@Override
	public void onEndStream(HttpExchange x) {
		// token门（FND29 zokermanager-02）：配置了Token则校验Authorization头，未通过已回401。
		if (!ApiToken.check(x))
			return;
		// 浏览器源防御：Origin 非同源（CSRF）或回环绑定下 Host 非回环（DNS rebinding）
		// 已回403——默认回环+无Token形态对浏览器代发请求设防。
		if (!BrowserOriginGuard.check(x))
			return;
		SearchBrowse.serve(x, true, "/api/browse");
	}
}
