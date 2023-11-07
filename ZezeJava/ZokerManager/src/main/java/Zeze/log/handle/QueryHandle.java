package Zeze.log.handle;

import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.LogAgent;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import Zeze.log.handle.entity.QueryParam;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

public class QueryHandle implements HttpEndStreamHandle {
    @Override
    public void onEndStream(HttpExchange x) throws Exception {
        try {
            ByteBuf content = x.content();
            int readableBytes = content.readableBytes();
            byte[] bytes = new byte[readableBytes];
            content.readBytes(bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);
            QueryParam queryParam = JSONObject.parseObject(str, QueryParam.class);
            LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
            String serverName = queryParam.getServerName();
            String result = logAgent.query(serverName, queryParam.getJson());
            x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(BaseResponse.succResult(result)));
        }catch (Exception e){
            x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(BaseResponse.errorResult("system error")));
            e.printStackTrace();
        }
    }
}
