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

			SocketAddress socketAddress = x.channel().remoteAddress();
			Object session = FileSessionManager.get(socketAddress);
			if (serverName != null && !serverName.trim().isEmpty()) {
				if (searchLogParam.isChangeSession() || !(session instanceof Session)) {
					session = logAgent.newSession(serverName, logName);
					x.setUserState(session);
					FileSessionManager.put(socketAddress, session);
				}
				BResult.Data data = ((Session)session).search(searchLogParam.getLimit(), searchLogParam.isReset(), con)
						.get(1, TimeUnit.MINUTES);
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			} else {
				if (searchLogParam.isChangeSession() || !(session instanceof SessionAll)) {
					session = logAgent.newSessionAll(logName);
					x.setUserState(session);
					FileSessionManager.put(socketAddress, session);
				}
				BResult.Data data = ((SessionAll)session).search(searchLogParam.getLimit(), searchLogParam.isReset(),
						con);
				x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.succResult(data)));
			}
		} catch (Exception e) {
			x.sendJson(HttpResponseStatus.OK, Json.toCompactString(BaseResponse.errorResult("system error")));
			e.printStackTrace();
		}
	}
}
