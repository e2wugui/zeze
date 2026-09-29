package Zeze.log.handle;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.Util.Json;
import Zeze.log.ApiToken;
import Zeze.log.BrowserOriginGuard;
import Zeze.log.FileSessionManager;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import Zeze.log.handle.entity.SearchLogParam;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * /api/search 处理器：按条件搜索日志（复用或重建查询会话）。
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
		try {

			ByteBuf content = x.content();
			int readableBytes = content.readableBytes();
			byte[] bytes = new byte[readableBytes];
			content.readBytes(bytes);
			String str = new String(bytes, StandardCharsets.UTF_8);
				SearchLogParam searchLogParam = Json.parse(str, SearchLogParam.class);
				// limit 下界校验（FND30 zokermanager-04）：漏传（JSON缺字段→默认0）/0/负值若透传，
				// 服务器把 limit<=0 当"翻页终结"——零扫描返回空成功 remain=false，与"查完无匹配"
				// 同形；且请求已先创建查询会话。入口即拒：明确 errorResult，不建/复用会话。
				// 上界不重复：服务器 clampLimit 已收敛。
				if (searchLogParam.getLimit() <= 0) {
					x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("invalid limit")));
					return;
				}
				// 参数级入口校验（与 limit 同根同款）：containsType 枚举/words+pattern 归一后双空——
				// 透传时服务端回非零码（与死会话同族），operateRecovering 会误判会话死亡整组拆建
				// 重试后仍恒失败且错误不可区分。入口即拒：明确 errorResult，不建/复用会话。
				var invalid = searchLogParam.validateError(false);
				if (invalid != null) {
					x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult(invalid)));
					return;
				}
				LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
			String serverName = searchLogParam.getServerName();
			String logName = searchLogParam.getLogName();

			BCondition.Data con = new BCondition.Data();
			con.setBeginTime(searchLogParam.parseBeginTime());
			con.setEndTime(searchLogParam.parseEndTime());
			con.getWords().addAll(searchLogParam.wordsToList());
			con.setContainsType(searchLogParam.getContainsType());
			con.setPattern(searchLogParam.getPattern());

			// 会话回执比对（FileSessionManager.resolve：三元组+allView键集快照，不匹配/漂移即关旧建新）
			// + 会话级错误驱逐重建重试一次（operateRecovering，zoker-03）：正确性不依赖前端
			// 记得置 changeSession，服务端空闲回收后的死会话不再恒 system error。
			SocketAddress socketAddress = x.channel().remoteAddress();
			if (serverName != null && !serverName.trim().isEmpty()) {
				// 单服务器名注册表预校验（对称 /api/query 的 LogAgent.query 显式判空）：未注册
				// （构造API/注册竞态/已被SM摘除）的名字透传到 Session 的裸解引用是 NPE——无信息、
				// 不满足会话级判别不自愈，恒 system error 且续页期绑定滞留 2h。归一 trim 后比对，
				// 未知名入口即拒：明确 errorResult，不建/复用会话。
				serverName = serverName.trim();
				if (!logAgent.getLogServers().contains(serverName)) {
					x.sendJson(HttpResponseStatus.OK,
							Json.toCompactString(BaseResponse.errorResult("unknown log server: " + serverName)));
					return;
				}
				BResult.Data data = FileSessionManager.operateRecovering(logAgent, socketAddress,
						searchLogParam.isChangeSession(), false, serverName, logName,
						session -> {
							x.setUserState(session);
							return ((Session)session).search(searchLogParam.getLimit(), searchLogParam.isReset(), con)
									.get(1, TimeUnit.MINUTES);
						});
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			} else {
				BResult.Data data = FileSessionManager.operateRecovering(logAgent, socketAddress,
						searchLogParam.isChangeSession(), true, null, logName,
						session -> {
							x.setUserState(session);
							return ((SessionAll)session).search(searchLogParam.getLimit(), searchLogParam.isReset(), con);
						});
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			}
		} catch (Session.InvalidArgumentException e) {
			// 服务端参数级拒绝（入口校验的兜底承载）：不拆会话，明确报参数错误而非 system error。
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("invalid search condition")));
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
