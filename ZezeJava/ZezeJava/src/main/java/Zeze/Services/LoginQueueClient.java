package Zeze.Services;

import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Builtin.LoginQueue.BQueuePosition;
import Zeze.Net.Service;
import Zeze.Util.Action0;
import Zeze.Util.Action1;

public class LoginQueueClient extends AbstractLoginQueueClient {
    private final LoginQueueClientService service;

    public LoginQueueClient() {
        service = new LoginQueueClientService();
        RegisterProtocols(service);
    }

    public void connect(String hostNameOrAddress, int port) {
        // XA1-F1停机屏障适配：ProcessPutLoginToken/ProcessPutQueueFull收到应答后自stop()
        // （一次性登录队列连接），本类的生命周期契约是"再次connect即复用"。屏障只在
        // Service.start()复位——对已停止服务建连会在addSocket被自查自关，autoReconnect
        // 退避循环反复撞同一屏障，token永不到达，调用方await永久挂起且全程静默。
        // connect前start()复活服务；幂等：首次connect时无acceptor/connector为空操作。
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

    private Action1<BQueuePosition.Data> queuePosition;
    private Action1<BLoginToken.Data> loginToken;
    private Action0 queueFull;

    public Action0 getQueueFull() {
        return queueFull;
    }

    public void setQueueFull(Action0 queueFull) {
        this.queueFull = queueFull;
    }

    public Action1<BQueuePosition.Data> getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Action1<BQueuePosition.Data> queuePosition) {
        this.queuePosition = queuePosition;
    }

    public Action1<BLoginToken.Data> getLoginToken() {
        return loginToken;
    }

    public void setLoginToken(Action1<BLoginToken.Data> loginToken) {
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
        // stop必须先行：服务端putLoginToken推完即closeGracefully，若回调先行——回调内可能
        // 阻塞等待（如ClientGame在回调里Connector.WaitReady等linkd连上，可长达数百ms）——
        // close触发的autoReconnect在窗口内重拨5020二次排队，可能再领到绑另一linkd的token
        // 并重跑回调，客户端同时挂两个linkd连接（GetSocket任取其一）→auth被choiceProvider拒r:2。
        // 先置停机屏障并停掉queue connector，在途/迟到的重拨要么被取消要么撞屏障自查自关。
        stop();
        if (null != loginToken)
            loginToken.run(p.Argument);
        return 0;
    }

    @Override
    protected long ProcessPutQueueFull(Zeze.Builtin.LoginQueue.PutQueueFull p) throws Exception {
        // 同ProcessPutLoginToken：stop先行，杜绝满员回调阻塞期间autoReconnect重拨二次排队
        stop();
        if (null != queueFull)
            queueFull.run();
        return 0;
    }
}
