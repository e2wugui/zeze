package Zeze.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
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
    // FND7-20：令牌的防伪完全依赖secretKey/secretIv只有LoginQueue与linkd知晓。原用
    // Zeze.Util.Random（ThreadLocalRandom，种子仅由时间源混合而来）生成，攻击者经过一次
    // 排队登录取得密文样本后可离线穷举种子并伪造任意serverId/expireTime的令牌绕过排队
    // 越权进入。必须用CSPRNG（对齐Zeze.Services.Token的做法）。
    private static final SecureRandom secureRandom = new SecureRandom();

    private static Binary nextSecretBinary() {
        var bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return new Binary(bytes);
    }

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
            this.secret.setSecretKey(nextSecretBinary());
            this.secret.setSecretIv(nextSecretBinary());
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

    // FND7-20遗留（复审R3决策文档，本轮不改令牌格式）：密钥已改SecureRandom，但IV随进程
    // 固定复用——AES-CBC确定性加密：同明文首块（serverId/linkServerId等定长字段）产出同密文
    // 首块，跨令牌泄露首块相等性（IND-CPA不成立）。修复决策：per-token随机IV前缀
    // （令牌=IV(16字节)||AES-CBC-PKCS5(key,IV,明文)），弃AES-GCM（nonce复用后果灾难性、
    // 无现成GCM管线，CBC+随机IV对本威胁模型已足够且是最小改动），弃"按天轮换IV"（需重发
    // AnnounceSecret+linkd双IV窗口，同为linkd联动且天内仍复用，劣于per-token）。
    // 迁移路径（需linkd联动，LinkdProvider.choiceProvider经decodeToken解码，故成文不动格式）：
    // ①先升级linkd解码端同时接受新旧格式（BToken编码定长，旧密文长度N固定、新格式长度
    //   16+N，按总长区分，不需版本字节不增开销）；
    // ②观察一个令牌过期窗（eLoginTokenExpireTime=30分钟）以上，保证在飞旧令牌全部消化；
    // ③最后升级LoginQueue编码端只产新格式。任意时刻可回退编码端回旧格式（旧格式全程可解）。
    // AnnounceSecret协议不变：secretKey仍16字节；secretIv在新格式下不再参与编码，迁移期
    // 保留供旧令牌解码，迁移完成后可从BSecret移除。
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
	protected long ProcessReportProviderLoad(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) throws Exception {
		r.getSender().setUserState(providers);
		providers.put(r.getSender(), r.Argument);
		loginQueue.tryResetTimeThrottle(providers.size());
		loginQueue.drainQueue(); // 上报可能让choiceProvider/choiceLink首次可用，立即给排队连接分配，不等1秒tick
		return 0;
	}

	@Override
	protected long ProcessReportLinkLoad(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) throws Exception {
		r.getSender().setUserState(links);
		links.put(r.getSender(), r.Argument);
		loginQueue.drainQueue();
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

    public static void main(String [] args) throws Exception {
        var secret = new BSecret.Data();
        secret.setSecretKey(nextSecretBinary());
        secret.setSecretIv(nextSecretBinary());

        var provider = new BToken.Data();
        provider.setLinkServerId(-1);
        provider.setServerId(0);
        provider.setSerialId(1);
        provider.setExpireTime(System.currentTimeMillis() + 5 * 60 * 1000);
        var encoded = encodeToken(secret, provider);
        var decoded = decodeToken(secret, encoded);
        System.out.println(decoded);
    }
}
