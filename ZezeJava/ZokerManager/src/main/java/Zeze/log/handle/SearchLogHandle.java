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
		try {

			ByteBuf content = x.content();
			int readableBytes = content.readableBytes();
			byte[] bytes = new byte[readableBytes];
			content.readBytes(bytes);
			String str = new String(bytes, StandardCharsets.UTF_8);
			SearchLogParam searchLogParam = Json.parse(str, SearchLogParam.class);
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
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
