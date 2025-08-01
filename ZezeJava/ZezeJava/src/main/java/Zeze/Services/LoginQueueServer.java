package Zeze.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Builtin.LoginQueue.BToken;
import Zeze.Builtin.LoginQueueServer.AnnounceSecret;
import Zeze.Builtin.LoginQueueServer.BSecret;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
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
        private final BSecret.Data secret;

        public LoginQueueService(Config config) {
            super("LoginQueueServer", config);
            this.secret = new BSecret.Data();
            this.secret.setSecretKey(Random.nextBinary(16));
            this.secret.setSecretIv(Random.nextBinary(16));
        }

        public BSecret.Data getSecret() {
            return secret;
        }

        @Override
        public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
            super.OnSocketAccept(so);
            var p = new AnnounceSecret(secret);
            p.Send(so);
        }

        @Override
        public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
            super.OnSocketClose(so, e);
            LoginQueueServer.this.onClose(so);
        }
    }

    public LoginQueueService getService() {
        return service;
    }

    public LoginQueueServer(LoginQueue loginQueue, Config config) {
        this.loginQueue = loginQueue;
        this.service = new LoginQueueService(config);
        RegisterProtocols(this.service);
    }

    void onClose(AsyncSocket so) {
        if (null != so.getUserState()) {
            @SuppressWarnings("unchecked") var loads = (Map<AsyncSocket, BServerLoad.Data>)so.getUserState();
            loads.remove(so);
            if (loads == providers)
                loginQueue.tryResetTimeThrottle(providers.size());
        }
    }

    public BSecret.Data getSecret() {
        return service.getSecret();
    }

    public static Binary encodeToken(BSecret.Data secret, BToken.Data provider) throws Exception {
        // provider 信息编码加密发送给客户端，再转给linkd使用。
        var bb = ByteBuffer.Allocate();
        provider.encode(bb);
        return new Binary(encrypt(secret, bb.Bytes, bb.ReadIndex, bb.size()));
    }


    public static BToken.Data decodeToken(BSecret.Data secret, Binary token) throws Exception {
        var bytes = decrypt(secret, token.bytesUnsafe(), token.getOffset(), token.size());
        var bb = ByteBuffer.Wrap(bytes);
        var provider = new BToken.Data();
        provider.decode(bb);
        return provider;
    }

    private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";

    public static byte[] encrypt(BSecret.Data secret, byte[] bytes, int offset, int size) throws Exception {
        var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
        var ivSpec = new IvParameterSpec(secret.getSecretIv().bytesUnsafe());

        var cipher = Cipher.getInstance(AES_CBC_PKCS5);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(bytes, offset, size);
    }

    public static byte[] decrypt(BSecret.Data secret, byte[] bytes, int offset, int size) throws Exception {
        var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
        var ivSpec = new IvParameterSpec(secret.getSecretIv().bytesUnsafe());

        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(bytes, offset, size);
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
