package Zeze.Services;

import java.util.Objects;
import java.util.ArrayList;
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
import Zeze.Util.Random;
import org.jspecify.annotations.Nullable;

/**
 * 登录队列服务端：向provider/linkd分发secret并接收负载上报，按负载选择服务器，
 * 签发加密登录令牌供LoginQueue分配。
 */
public class LoginQueueServer extends AbstractLoginQueueServer {
    // 使用密码学安全随机数生成令牌密钥，防止被离线预测伪造
    private static final SecureRandom secureRandom = new SecureRandom();

    private static Binary nextSecretBinary() {
        var bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return new Binary(bytes);
    }

    private final ConcurrentHashMap<AsyncSocket, BServerLoad.Data> providers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AsyncSocket, BServerLoad.Data> links = new ConcurrentHashMap<>();
    // 串行化负载登记、限流重算与断连清理，配合锁内判活防止迟到上报复活已断连条目
    private final ReentrantLock editLock = new ReentrantLock();
    private final LoginQueueService service;
    private final LoginQueue loginQueue;

    /** 锁内判活：连接已从 socketMap 摘除则说明清理将执行或已完成，应丢弃迟到上报。 */
    private boolean isSenderAlive(AsyncSocket sender) {
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
        public void OnSocketAccept(AsyncSocket so) throws Exception {
            super.OnSocketAccept(so);
            var p = new AnnounceSecret(secret);
            p.Send(so);
        }

        @Override
        public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
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
            // 两表都幂等移除，不依赖 userState 指向，防止同连接上报过多种负载时漏清
            if (providers.remove(so) != null)
                loginQueue.tryResetTimeThrottle(providers.size());
            if (links.remove(so) != null)
                loginQueue.tryResetTimeThrottle(providers.size());
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


    public static BToken.Data decodeToken(BSecret.Data secret, Binary token) {
        // 优先按随机 IV 前缀格式解码，失败再回退兼容固定 secretIv 旧格式
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

    /** 单格式解密、解码并做基本语义校验，失败返回 null。 */
    private static BToken.@Nullable Data tryDecodeToken(BSecret.Data secret, byte[] bytes, int offset, int size,
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
            // 过期时间恒为正，用于区分双试探误解；完整新鲜度与 linkServerId 校验由调用方执行
            if (provider.getExpireTime() <= 0)
                return null;
            return provider;
        } catch (Throwable e) {
            return null;
        }
    }

    // 令牌密文格式：IV(16字节) || AES-CBC-PKCS5(key, IV, 明文)；解码端同时兼容旧版固定 secretIv 格式
    private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
    private static final int TOKEN_IV_SIZE = 16;
    private static final SecureRandom tokenIvRandom = new SecureRandom();

    public static byte[] encrypt(BSecret.Data secret, byte[] bytes, int offset, int size) throws Exception {
        var keySpec = new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES");
        // 每个令牌使用独立随机 IV 前缀，避免固定 IV 的确定性加密
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

    /** 兼容旧格式解密（使用固定 secretIv）。 */
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
			if (!isSenderAlive(Objects.requireNonNull(r.getSender()))) // 迟到上报，会话已清理——拒绝防死服务器复活
				return 0;
			r.getSender().setUserState(providers);
			providers.put(r.getSender(), r.Argument);
			loginQueue.tryResetTimeThrottle(providers.size());
		} finally {
			editLock.unlock();
		}
		loginQueue.drainQueue(); // 上报可能让choiceProvider/choiceLink首次可用，立即给排队连接分配，不等1秒tick
		return 0;
	}

	@Override
	protected long ProcessReportLinkLoad(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) throws Exception {
		editLock.lock();
		try {
			if (!isSenderAlive(Objects.requireNonNull(r.getSender()))) // 同上
				return 0;
			r.getSender().setUserState(links);
			links.put(r.getSender(), r.Argument);
			loginQueue.tryResetTimeThrottle(providers.size());
		} finally {
			editLock.unlock();
		}
		loginQueue.drainQueue();
		return 0;
	}

    public int providerSize() {
        return providers.size();
    }

	    public int linkSize() {
	        return links.size();
	    }

    public BServerLoad.@Nullable Data choiceLink() {
        return choiceServer(links);
    }

    public BServerLoad.@Nullable Data choiceProvider() {
        return choiceServer(providers);
    }

    /**
     * 根据负载选择服务器。
     * @see Zeze.Arch.ProviderDistribute::choiceLoad
     * @param servers 服务器
     * @return 返回分配的服务，null表示选择失败。
     */
    private static BServerLoad.@Nullable Data choiceServer(ConcurrentHashMap<AsyncSocket, BServerLoad.Data> servers) {
        var totalWeight = 0L;
        var frees = new ArrayList<FreeServer>(servers.size());
        for (var e : servers.entrySet()) {
            var load = e.getValue().getLoad();
            if (load.getOverload() == BLoad.eOverload)
                continue;
            if (load.getOnlineNew() > load.getMaxOnlineNew())
                continue;
            long weight = load.getProposeMaxOnline() - load.getOnline();
            if (weight <= 0)
                continue;
            frees.add(new FreeServer(e.getKey(), e.getValue(), weight));
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            var randWeight = Random.getInstance().nextLong(totalWeight);
            for (var ps : frees) {
                if (randWeight < ps.weight) {
                    incrementOnline(servers, ps.socket);
                    return ps.data;
                }
                randWeight -= ps.weight;
            }
        }
        // 选择失败
        return null;
    }

    private record FreeServer(AsyncSocket socket, BServerLoad.Data data, long weight) {
    }

    // 使用 CHM.compute 保证与 ReportProviderLoad 的整体替换互斥，避免更新丢失在旧对象上
    private static void incrementOnline(ConcurrentHashMap<AsyncSocket, BServerLoad.Data> servers,
                                        AsyncSocket socket) {
        servers.compute(socket, (k, data) -> {
            if (data != null) {
                var load = data.getLoad();
                load.setOnline(load.getOnline() + 1);
            }
            return data;
        });
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
