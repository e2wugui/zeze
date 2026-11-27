package Zeze.Services;

import org.jspecify.annotations.Nullable;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Builtin.LoginQueue.BQueuePosition;
import Zeze.Net.Service;
import Zeze.Util.Action0;
import Zeze.Util.Action1;

/**
 * 登录队列客户端：连接LoginQueue排队，回调队列位置/满员/登录令牌。
 */
public class LoginQueueClient extends AbstractLoginQueueClient {
    private final LoginQueueClientService service;

    public LoginQueueClient() {
        service = new LoginQueueClientService();
        RegisterProtocols(service);
    }

    public void connect(String hostNameOrAddress, int port) {
        // 收到令牌或满员后会自 stop()，再次 connect 前需先 start() 复位服务停机屏障
        try {
            service.start();
        } catch (Exception e) {
            throw Zeze.Util.Task.forceThrow(e);
        }
        service.connect(hostNameOrAddress, port, true);
    }

    public void stop() throws Exception {
        service.stop();
    }

	public void start() throws Exception {
		service.start();
	}

    public static class LoginQueueClientService extends Service {
        public LoginQueueClientService()
        {
            super("LoginQueueClient");
        }
    }

    private @Nullable Action1<BQueuePosition.Data> queuePosition;
    private @Nullable Action1<BLoginToken.Data> loginToken;
    private @Nullable Action0 queueFull;

    public @Nullable Action0 getQueueFull() {
        return queueFull;
    }

    public void setQueueFull(@Nullable Action0 queueFull) {
        this.queueFull = queueFull;
    }

    public @Nullable Action1<BQueuePosition.Data> getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(@Nullable Action1<BQueuePosition.Data> queuePosition) {
        this.queuePosition = queuePosition;
    }

    public @Nullable Action1<BLoginToken.Data> getLoginToken() {
        return loginToken;
    }

    public void setLoginToken(@Nullable Action1<BLoginToken.Data> loginToken) {
        this.loginToken = loginToken;
    }

    @Override
    protected long ProcessPutQueuePosition(Zeze.Builtin.LoginQueue.PutQueuePosition p) throws Exception {
        if (null != queuePosition)
            queuePosition.run(p.Argument);
        return 0;
    }

    @Override
    protected long ProcessPutLoginToken(Zeze.Builtin.LoginQueue.PutLoginToken p) throws Exception {
        // 先停止连接器再触发回调，防止回调阻塞期间服务端断连触发 autoReconnect 二次排队
        stop();
        if (null != loginToken)
            loginToken.run(p.Argument);
        return 0;
    }

    @Override
    protected long ProcessPutQueueFull(Zeze.Builtin.LoginQueue.PutQueueFull p) throws Exception {
        // 先停止连接器再触发回调，防止满员回调期间自动重连二次排队
        stop();
        if (null != queueFull)
            queueFull.run();
        return 0;
    }
}
