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
        service.connect(hostNameOrAddress, port, false);
    }

    public void close() throws Exception {
        service.stop();
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
        if (null != loginToken)
            loginToken.run(p.Argument);
        close();
        return 0;
    }

    @Override
    protected long ProcessPutQueueFull(Zeze.Builtin.LoginQueue.PutQueueFull p) throws Exception {
        if (null != queueFull)
            queueFull.run();
        close();
        return 0;
    }
}
