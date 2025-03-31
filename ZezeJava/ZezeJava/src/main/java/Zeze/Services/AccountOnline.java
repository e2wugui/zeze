package Zeze.Services;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.AccountOnline.BAccountLink;
import Zeze.Builtin.AccountOnline.Kick;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.FastLock;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AccountOnline extends AbstractAccountOnline {
    private final AccountOnlineService service;
    private final ConcurrentHashMap<String, AsyncSocket> links = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AccountInfo> accounts = new ConcurrentHashMap<>();

    public static void main(String []args) throws Exception {
        Task.tryInitThreadPool();
        ZezeCounter.tryInit();
        var server = new AccountOnline(Config.load("accountOnline.xml"));
        try {
            server.start();
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait();
            }
        } finally {
            server.stop();
        }
    }

    public AccountOnline(Config config) {
        service = new AccountOnlineService(config);
        RegisterProtocols(service);
    }

    public void start() throws Exception {
        service.start();
    }

    public void stop() throws Exception {
        service.stop();
    }

    private void kick(BAccountLink.Data link) {
        var r = new Kick();
        r.Argument = link;
        r.Send(links.get(link.getLinkName())); // 不等待结果。
    }

    @Override
    protected long ProcessLoginRequest(Zeze.Builtin.AccountOnline.Login r) {
        while (true) {
            var info = accounts.computeIfAbsent(r.Argument.getAccountLink().getAccount(), (key) -> new AccountInfo());
            info.lock();
            try {
                if (info.removed)
                    continue; // 拿到了被删除的记录，重新获取一个有效的。

                if (info.link == null || info.link.equals(r.Argument.getAccountLink())) {
                    // 新鲜的Login或者是自己.
                    info.link = r.Argument.getAccountLink();
                    r.SendResult();
                    return 0;
                }

                if (r.Argument.isKickOld()) {
                    // 存在旧的登录。踢掉。
                    kick(info.link);
                    info.link = r.Argument.getAccountLink();
                    r.SendResult();
                    return 0;
                }
                // 存在旧的登录，这次登录失败。
                r.SendResultCode(Procedure.DuplicateRequest);
                return 0;
            } finally {
                info.unlock();
            }
        }
    }

    @Override
    protected long ProcessLogoutRequest(Zeze.Builtin.AccountOnline.Logout r) {
        while (true) {
            var info = accounts.get(r.Argument.getAccount());
            if (info == null) {
                r.SendResult();
                return 0;
            }
            info.lock();
            try {
                if (info.removed)
                    continue;

                if (info.link != null && info.link.equals(r.Argument)) {
                    info.removed = true;
                    accounts.remove(r.Argument.getAccount());
                }
                r.SendResult();
                return 0;
            } finally {
                info.unlock();
            }
        }
    }

    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.AccountOnline.Register r) {
        r.getSender().setUserState(r.Argument.getLinkName());
        links.put(r.Argument.getLinkName(), r.getSender());
        r.SendResult();
        return 0;
    }

    public static class AccountInfo extends FastLock {
        private boolean removed = false;
        private BAccountLink.Data link;
    }

    public class AccountOnlineService extends Service {
        public AccountOnlineService(Config config) {
            super("Zeze.Services.AccountOnline", config);
            setNoProcedure(true);
        }

        @Override
        public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
            if (so.getUserState() != null) {
                links.remove((String)so.getUserState(), so);
            }
            super.OnSocketClose(so, e);
        }
    }
}
