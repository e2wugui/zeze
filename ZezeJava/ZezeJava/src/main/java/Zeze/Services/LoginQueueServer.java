package Zeze.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
    // S3-F1：负载登记与关闭清理的串行锁（对齐姊妹类ServiceManagerServer的editLock+FND5-29
    // 判活纪律）。Report*经DispatchMode.Normal跑在worker线程，与IO线程的OnSocketClose竞态：
    // 迟到的上报在清理remove之后重新put即产生永久幽灵条目（死服务器持续被choice分配且
    // 永不回收）。锁内判活（GetSocket==sender）+锁内清理互相串行化后：判活通过⟹清理未开始
    // （NetServer.OnSocketClose先从socketMap摘除再回调onClose，摘除后判活必失败），
    // 本次put会被随后的清理收走；判活失败⟹会话已死，拒绝即不产生残留。
    private final ReentrantLock editLock = new ReentrantLock();
    private final LoginQueueService service;
    private final LoginQueue loginQueue;

    /** S3-F1：锁内判活（见editLock注释）。死会话的迟到上报静默丢弃（Report*非Rpc，
     * 死连接本就收不到应答）。 */
    private boolean isSenderAlive(@NotNull AsyncSocket sender) {
        return service.GetSocket(sender.getSessionId()) == sender;
    }

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
        editLock.lock();
        try {
            // 【FND11 svc-02】同一socket先后上报provider与link负载时userState只指向最后一张
            // 表，原先仅清userState指向的表——另一张表条目永久残留（socket已关无后续上报，
            // 无回收路径），幽灵服务器持续参与choiceServer权重选择并虚高providerSize。两表
            // 都幂等remove，不再依赖userState指向。
            if (providers.remove(so) != null)
                loginQueue.tryResetTimeThrottle(providers.size());
            links.remove(so);
        } finally {
            editLock.unlock();
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
        // 双试探+语义校验（FND8-65，判别式见下方迁移注释）：先按新格式（IV=前16B）解并验
        // BToken合法性与语义，失败再按旧格式（secretIv）解并验，均败则拒。
        var bytes = token.bytesUnsafe();
        var offset = token.getOffset();
        var size = token.size();
        var provider = tryDecodeToken(secret, bytes, offset, size, true);
        if (provider == null)
            provider = tryDecodeToken(secret, bytes, offset, size, false);
        if (provider == null)
            throw new IllegalArgumentException("decode token fail: neither iv-prefixed nor legacy format");
        return provider;
    }

    /** 单格式解密+解码+语义校验，任一步失败返回null（供双试探判别）。 */
    private static BToken.Data tryDecodeToken(BSecret.Data secret, byte[] bytes, int offset, int size,
                                              boolean ivPrefixed) {
        try {
            byte[] plain;
            if (ivPrefixed) {
                if (size <= TOKEN_IV_SIZE)
                    return null;
                var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
                var cipher = Cipher.getInstance(AES_CBC_PKCS5);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(bytes, offset, TOKEN_IV_SIZE));
                plain = cipher.doFinal(bytes, offset + TOKEN_IV_SIZE, size - TOKEN_IV_SIZE);
            } else {
                plain = decrypt(secret, bytes, offset, size);
            }
            var provider = new BToken.Data();
            provider.decode(ByteBuffer.Wrap(plain));
            // 语义校验：expireTime由发放端固定为now+eLoginTokenExpireTime，恒为正；
            // 新鲜度与linkServerId==本机的完整语义校验由调用方（LinkdProvider）执行。
            if (provider.getExpireTime() <= 0)
                return null;
            return provider;
        } catch (Throwable e) {
            return null;
        }
    }

    // FND8-65（FND7-20复审R3既定决策落地）：IV不再随进程固定复用——固定IV的AES-CBC是
    // 确定性加密（IND-CPA不成立）。对抗复核已证伪具体的跨令牌泄露通道（BToken首字段是
    // 每令牌唯一递增的serialId，任意两令牌首块明文必不同），本修复属密码学卫生。
    // 令牌格式改为 IV(16字节)||AES-CBC-PKCS5(key,IV,明文)；弃AES-GCM（nonce复用后果
    // 灾难性、无现成GCM管线，CBC+随机IV对本威胁模型已足够且是最小改动），弃"按天轮换IV"
    // （需重发AnnounceSecret+linkd双IV窗口，同为linkd联动且天内仍复用，劣于per-token）。
    // 迁移判别（对抗修正：BToken是变长编码，旧密文可为16B或32B，新格式总长=16+旧，
    // 32B在双格式并存窗口二义，纯长度判别不成立）：解码端双试探+语义校验（见decodeToken），
    // 误判只会落在已过期令牌上，由expireTime新鲜度+linkServerId==本机校验兜底；
    // 旧格式全程可解（编码端可随时回退）。
    // AnnounceSecret协议不变：secretKey仍16字节；secretIv在新格式下不再参与编码，
    // 迁移期保留供旧令牌解码，全部升级后可从BSecret移除。
    private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
    private static final int TOKEN_IV_SIZE = 16;
    private static final SecureRandom tokenIvRandom = new SecureRandom();

    public static byte[] encrypt(BSecret.Data secret, byte[] bytes, int offset, int size) throws Exception {
        var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
        // per-token随机IV前缀（FND8-65）：输出=IV||密文
        var iv = new byte[TOKEN_IV_SIZE];
        tokenIvRandom.nextBytes(iv);

        var cipher = Cipher.getInstance(AES_CBC_PKCS5);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        var encrypted = cipher.doFinal(bytes, offset, size);

        var out = new byte[TOKEN_IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, out, 0, TOKEN_IV_SIZE);
        System.arraycopy(encrypted, 0, out, TOKEN_IV_SIZE, encrypted.length);
        return out;
    }

    /** 旧格式解密（固定secretIv）：仅剩迁移期解码用途（decodeToken双试探的旧分支）。 */
    public static byte[] decrypt(BSecret.Data secret, byte[] bytes, int offset, int size) throws Exception {
        var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
        var ivSpec = new IvParameterSpec(secret.getSecretIv().bytesUnsafe());

        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(bytes, offset, size);
    }

	@Override
	protected long ProcessReportProviderLoad(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) throws Exception {
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) // S3-F1：迟到上报，会话已清理——拒绝防死服务器复活
				return 0;
			r.getSender().setUserState(providers);
			providers.put(r.getSender(), r.Argument);
		} finally {
			editLock.unlock();
		}
		loginQueue.tryResetTimeThrottle(providers.size());
		loginQueue.drainQueue(); // 上报可能让choiceProvider/choiceLink首次可用，立即给排队连接分配，不等1秒tick
		return 0;
	}

	@Override
	protected long ProcessReportLinkLoad(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) throws Exception {
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) // S3-F1：同上
				return 0;
			r.getSender().setUserState(links);
			links.put(r.getSender(), r.Argument);
		} finally {
			editLock.unlock();
		}
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
