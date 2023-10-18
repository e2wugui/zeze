package Zeze.log.handle;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.log.FileSessionManager;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import Zeze.log.handle.entity.SearchLogParam;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class BrowseLogHandle implements HttpEndStreamHandle {
    @Override
    public void onEndStream(HttpExchange x) throws Exception {
        try {
            ByteBuf content = x.content();
            int readableBytes = content.readableBytes();
            byte[] bytes = new byte[readableBytes];
            content.readBytes(bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);
            SearchLogParam searchLogParam = JSONObject.parseObject(str, SearchLogParam.class);
            LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
            String serverName = searchLogParam.getServerName();

            BCondition.Data con = new BCondition.Data();
            con.setBeginTime(searchLogParam.parseBeginTime());
            con.setEndTime(searchLogParam.parseEndTime());
            con.getWords().addAll(searchLogParam.wordsToList());
            con.setContainsType(searchLogParam.getContainsType());
            con.setPattern(searchLogParam.getPattern());

            if (serverName != null && !serverName.trim().isEmpty()){
                SocketAddress socketAddress = x.channel().remoteAddress();
                Object session = FileSessionManager.get(socketAddress);
                if (searchLogParam.isChangeSession() || session == null || !(session instanceof Session)){
                    session = logAgent.newSession(serverName);
                    x.setUserState(session);
                    FileSessionManager.put(socketAddress, session);
                }
                BResult.Data data = ((Session)session).browse(searchLogParam.getLimit(), searchLogParam.getOffsetFactor(),
                        searchLogParam.isReset(), con).get(1, TimeUnit.MINUTES);
                x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(BaseResponse.succResult(data)));

            }else {
                SocketAddress socketAddress = x.channel().remoteAddress();
                Object session = FileSessionManager.get(socketAddress);
                if (searchLogParam.isChangeSession() || session == null || !(session instanceof SessionAll)){
                    session = logAgent.newSessionAll();
                    x.setUserState(session);
                    FileSessionManager.put(socketAddress, session);
                }
                BResult.Data data = ((SessionAll)session).browse(searchLogParam.getLimit(), searchLogParam.getOffsetFactor(),
                        searchLogParam.isReset(), con);
                x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(BaseResponse.succResult(data)));
            }
        }catch (Exception e){
            x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(BaseResponse.errorResult("system error")));
            e.printStackTrace();
        }
    }
}
