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
import Zeze.log.FileSessionManager;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import Zeze.log.handle.entity.SearchLogParam;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

public class SearchLogHandle implements HttpEndStreamHandle {
	@Override
	public void onEndStream(HttpExchange x) {
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

			// GE-D06 会话回执比对：复用会话前比对请求的(会话类型, serverName, logName)与绑定记录，
			// 不匹配（或changeSession强制重建）时关旧建新——正确性不再依赖前端记得置changeSession。
			SocketAddress socketAddress = x.channel().remoteAddress();
			if (serverName != null && !serverName.trim().isEmpty()) {
				Session session = (Session)FileSessionManager.resolve(logAgent, socketAddress,
						searchLogParam.isChangeSession(), false, serverName, logName);
				x.setUserState(session);
				BResult.Data data = session.search(searchLogParam.getLimit(), searchLogParam.isReset(), con)
						.get(1, TimeUnit.MINUTES);
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			} else {
				SessionAll session = (SessionAll)FileSessionManager.resolve(logAgent, socketAddress,
						searchLogParam.isChangeSession(), true, null, logName);
				x.setUserState(session);
				BResult.Data data = session.search(searchLogParam.getLimit(), searchLogParam.isReset(),
						con);
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			}
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
