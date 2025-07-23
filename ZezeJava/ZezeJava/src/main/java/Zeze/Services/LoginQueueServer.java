package Zeze.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.LoginQueueServer.AnnounceSecret;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Util.KV;
import Zeze.Util.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoginQueueServer extends AbstractLoginQueueServer {
    private final ConcurrentHashMap<AsyncSocket, BServerLoad.Data> providers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AsyncSocket, BServerLoad.Data> links = new ConcurrentHashMap<>();
    private final LoginQueueService service;
    private final LoginQueue loginQueue;

    /**
     * 网络服务类 Acceptor
     * 接受provider和link连接。
     */
    public class LoginQueueService extends Service {
        private final Binary secretKey;

        public LoginQueueService() {
            super("LoginQueueServer");
            this.secretKey = Random.nextBinary(32);
        }

        public Binary getSecretKey() {
            return secretKey;
        }

        @Override
        public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
            super.OnSocketAccept(so);
            var p = new AnnounceSecret();
            p.Argument.setSecretKey(secretKey);
            p.Send(so);
        }

        @Override
        public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
            super.OnSocketClose(so, e);
            if (null != so.getUserState()) {
                @SuppressWarnings("unchecked") var loads = (Map<AsyncSocket, BServerLoad.Data>)so.getUserState();
                loads.remove(so);
            }
        }
    }

    public LoginQueueServer(LoginQueue loginQueue) {
        this.loginQueue = loginQueue;
        this.service = new LoginQueueService();
        RegisterProtocols(this.service);
    }

    public Binary getSecretKey() {
        return service.getSecretKey();
    }

    @Override
    protected long ProcessReportProviderLoad(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) {
        r.getSender().setUserState(providers);
        providers.put(r.getSender(), r.Argument);
        loginQueue.tryResetTimeThrottle(providers.size());
        return 0;
    }

    @Override
    protected long ProcessReportLinkLoad(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) {
        r.getSender().setUserState(links);
        links.put(r.getSender(), r.Argument);
        return 0;
    }

    public int providerSize() {
        return providers.size();
    }

    public BServerLoad.Data choiceLink() {
        return choiceServer(links);
    }

    public BServerLoad.Data choiceProvider() {
        return choiceServer(providers);
    }

    /**
     * 根据负载选择服务器。
     * @see Zeze.Arch.ProviderDistribute::choiceLoad
     * @param servers 服务器
     * @return 返回分配的服务，null表示选择失败。
     */
    private static BServerLoad.Data choiceServer(Map<AsyncSocket, BServerLoad.Data> servers) {
        var totalWeight = 0L;
        var frees = new ArrayList<KV<BServerLoad.Data, Long>>(servers.size());
        for (var e : servers.entrySet()) {
            var load = e.getValue().getLoad();
            if (load.getOverload() == BLoad.eOverload)
                continue;
            if (load.getOnlineNew() > load.getMaxOnlineNew())
                continue;
            long weight = load.getProposeMaxOnline() - load.getOnline();
            if (weight <= 0)
                continue;
            frees.add(KV.create(e.getValue(), weight));
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            var randWeight = Random.getInstance().nextLong(totalWeight);
            for (var ps : frees) {
                var weight = ps.getValue();
                if (randWeight < weight) {
                    ps.getKey().getLoad().setOnline(ps.getKey().getLoad().getOnline() + 1);
                    return ps.getKey();
                }
                randWeight -= weight;
            }
        }
        // 选择失败
        return null;
    }
}
