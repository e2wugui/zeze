package Zeze.log.handle;

import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Services.LogAgent;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.entity.BaseResponse;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.Set;

public class GetLogServersHandle implements HttpEndStreamHandle {
    @Override
    public void onEndStream(HttpExchange x) throws Exception {

        LogAgent logAgent = LogAgentManager.getInstance().getLogAgent();
        Set<String> logServers = logAgent.getLogServers();
        BaseResponse baseResponse = BaseResponse.succResult(new ArrayList<>(logServers));
        x.sendJson(HttpResponseStatus.OK, JSONObject.toJSONString(baseResponse));


    }
}
