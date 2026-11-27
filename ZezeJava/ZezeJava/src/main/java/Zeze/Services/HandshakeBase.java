package Zeze.Services;

import java.util.Objects;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Digest;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.BSHandshake0Argument;
import Zeze.Services.Handshake.CHandshake;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Handshake.Helper;
import Zeze.Services.Handshake.SHandshake;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Cert;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 连接加密握手基类：实现DH/RSA密钥交换与压缩协商协议族（CHandshake/SHandshake0/SHandshake/
 * CHandshakeDone/KeepAlive），握手完成以OnHandshakeDone回调；子类选择承担服务端/客户端角色。
 */
public class HandshakeBase extends Service {
	private static final Logger logger = LogManager.getLogger(HandshakeBase.class);

	private boolean serverRole; // 构造期由 addHandshakeServerFactoryHandle 置位，区分服务端角色

	static class Context {
		final Object context;
		final int encryptType; // 本次握手请求使用的加密类型，用于校验 SHandshake 回显一致
		@Nullable Future<?> timeoutTask;

		Context(Object context, int encryptType) {
			this.context = context;
			this.encryptType = encryptType;
		}
	}

	// 仅客户端使用
	private final LongConcurrentHashMap<Context> dhContext = new LongConcurrentHashMap<>();

	public HandshakeBase(String name, @Nullable Config config) {
		super(name, config);
	}

	public HandshakeBase(String name, @Nullable Application app) {
		super(name, app);
	}

	/**
	 * 连接数上限检查：覆写 OnSocketAccept 的子类须在入口调用本方法，超限时抛出 IllegalStateException 以关闭连接。
	 */
	protected final void checkMaxConnections() {
		if (getSocketCount() >= getConfig().getMaxConnections())
			throw new IllegalStateException("too many connections");
	}

	/**
	 * 服务端启用 Aes 模式但未配置 SecureIp 时告警：NAT/端口映射下两侧地址不一致会导致密钥派生失配。
	 */
	private void checkAesSecureIp() {
		var options = getConfig().getHandshakeOptions();
		if (options.getEncryptType() == Constant.eEncryptTypeAes && options.getSecureIp() == null) {
			// 仅服务端告警（客户端密钥派生不读 SecureIp）
			if (!serverRole)
				return;
			logger.warn("{} EncryptType=Aes without SecureIp: session key is derived from the connection "
					+ "address (server side: local address; client side: remote address). Direct connections work, "
					+ "but under NAT/port-mapping the two sides see different addresses and every handshake will "
					+ "fail with reconnect loops. Configure SecureIp on the server with the address clients dial in, "
					+ "or switch EncryptType to AesNoSecureIp(2)/RsaAes(3).", getName());
		}
	}

	@Override
	public void start() throws Exception {
		checkAesSecureIp();
		super.start();
	}

	@Override
	public boolean isHandshakeProtocol(long typeId) {
		return typeId == CHandshake.TypeId_ || typeId == SHandshake0.TypeId_ || typeId == SHandshake.TypeId_
				|| typeId == CHandshakeDone.TypeId_ || typeId == KeepAlive.TypeId_;
	}

	// 加密模式下同时装配解码准入门禁，已有同 TypeId 工厂则保留（如自定义 KeepAlive）
	private void addHandshakeFactoryHandle(long typeId, Service.ProtocolFactoryHandle<? extends Protocol<?>> handle) {
		if (getConfig().getHandshakeOptions().getEncryptType() != Constant.eEncryptTypeDisable)
			armDecodeAdmission(typeId);
		if (!getFactorys().containsKey(typeId))
			AddFactoryHandle(typeId, handle);
	}

	protected final void addHandshakeServerFactoryHandle() {
		serverRole = true;
		addHandshakeFactoryHandle(CHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshake::new, this::processCHandshake, TransactionLevel.None, DispatchMode.Direct));
		addHandshakeFactoryHandle(CHandshakeDone.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshakeDone::new, this::processCHandshakeDone, TransactionLevel.None, DispatchMode.Direct));
		addHandshakeFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
				HandshakeBase::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));
	}

	protected final void addHandshakeClientFactoryHandle() {
		addHandshakeFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake0::new, this::processSHandshake0, TransactionLevel.None, DispatchMode.Direct));
		addHandshakeFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake::new, this::processSHandshake, TransactionLevel.None, DispatchMode.Direct));
		addHandshakeFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
				HandshakeBase::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));
	}

	private static long processKeepAliveRequest(KeepAlive r) {
		r.SendResult();
		return 0L;
	}

	private long processCHandshakeDone(CHandshakeDone p) {
		try {
			// 握手完成是连接生命周期的一次转移；重复通知不能重建业务会话或重复绑定。
			if (Objects.requireNonNull(p.getSender()).isHandshakeDone())
				return 0L;
			//noinspection DataFlowIssue
			(((TcpSocket)p.getSender())).verifySecurity();
			OnHandshakeDone(p.getSender());
			return 0L;
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			//noinspection DataFlowIssue
			p.getSender().close(ex);
			return 0L;
		}
	}

	private int serverCompressS2c(int s2cHint) {
		var options = getConfig().getHandshakeOptions();
		if (options.getCompressS2c() != Constant.eCompressTypeDisable) { // 必须选择一种压缩
			if (options.isSupportedCompress(s2cHint)) // 优先客户端选择的压缩,但服务器需支持
				return s2cHint;
		} else if (s2cHint == Constant.eCompressTypeDisable || options.isSupportedCompress(s2cHint)) // 服务器可以不压缩时,优先客户端的选择,但服务器需支持
			return s2cHint;
		return Constant.eCompressTypeMppc; // 跟客户端选择的不兼容,就强制以MPPC作为兜底的压缩,客户端如果还不支持就无法继续通信了
	}

	private int serverCompressC2s(int c2sHint) {
		var options = getConfig().getHandshakeOptions();
		if (options.getCompressC2s() != Constant.eCompressTypeDisable) { // 必须选择一种压缩
			if (options.isSupportedCompress(c2sHint)) // 优先客户端选择的压缩,但服务器需支持
				return c2sHint;
		} else if (c2sHint == Constant.eCompressTypeDisable || options.isSupportedCompress(c2sHint)) // 服务器可以不压缩时,优先客户端的选择,但服务器需支持
			return c2sHint;
		return Constant.eCompressTypeMppc; // 跟客户端选择的不兼容,就强制以MPPC作为兜底的压缩,客户端如果还不支持就无法继续通信了
	}

	@SuppressWarnings("ConstantConditions") // 保留对解密实现返回值的防御检查。
	private long processCHandshake(CHandshake p) {
		try {
			// 拒绝已加密连接上的重复握手，防止重装编解码链泄漏旧资源
			if (p.getSender() instanceof TcpSocket tcp && tcp.isSecurity())
				throw new IllegalStateException("re-handshake on secured connection");

			// 校验客户端上报的加密类型与服务端配置一致，防止降级篡改
			if (p.Argument.encryptType != getConfig().getHandshakeOptions().getEncryptType())
				throw new IllegalStateException("encryptType mismatch: " + p.Argument.encryptType
						+ " expect " + getConfig().getHandshakeOptions().getEncryptType());

			byte[] inputKey = null;
			byte[] outputKey = null;
			byte[] response = ByteBuffer.Empty;
			int group = 1;
			switch (p.Argument.encryptType) {
			case Constant.eEncryptTypeAes: {
				// 当group采用客户端参数时需要检查参数正确性，现在统一采用了1，不需要检查了。
				BigInteger data = new BigInteger(p.Argument.encryptParam);
				BigInteger rand = Helper.makeDHRandom();
				byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
				//noinspection DataFlowIssue
				var localInet = (((TcpSocket)p.getSender())).getLocalInet();
				byte[] key = getConfig().getHandshakeOptions().getSecureIp() != null
						? getConfig().getHandshakeOptions().getSecureIp()
						: (localInet != null ? localInet.getAddress().getAddress() : ByteBuffer.Empty);
				logger.debug("{} localIp={}", p.getSender().getSessionId(), Arrays.toString(key));
				int half = material.length / 2;

				inputKey = Digest.hmacMd5(key, material, 0, half);
				response = Helper.generateDHResponse(group, rand).toByteArray();
				outputKey = Digest.hmacMd5(key, material, half, material.length - half);
				break;
			}
			case Constant.eEncryptTypeAesNoSecureIp: {
				BigInteger data = new BigInteger(p.Argument.encryptParam);
				BigInteger rand = Helper.makeDHRandom();
				byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
				int half = material.length / 2;

				inputKey = Arrays.copyOfRange(material, 0, half);
				response = Helper.generateDHResponse(group, rand).toByteArray();
				outputKey = Arrays.copyOfRange(material, half, material.length);
				break;
			}
			case Constant.eEncryptTypeRsaAes:
				var rsaPriKey = getConfig().getHandshakeOptions().getRsaPriKey();
				if (rsaPriKey == null)
					throw new IllegalStateException("need RsaPriKeyFile in ServiceConf");
				inputKey = Cert.decryptRsa(rsaPriKey, p.Argument.encryptParam);
				if (inputKey == null || inputKey.length != 64) {
					throw new IllegalStateException("invalid secret length = "
							+ (inputKey != null ? inputKey.length : -1));
				}
				outputKey = Arrays.copyOfRange(inputKey, 32, 64);
				break;
			}
			var s2c = serverCompressS2c(p.Argument.compressS2c);
			var c2s = serverCompressC2s(p.Argument.compressC2s);
			((TcpSocket)p.getSender()).setInputSecurityCodec(p.Argument.encryptType, inputKey, c2s);

			var sHandshake = new SHandshake();
			sHandshake.Argument.encryptParam = response;
			sHandshake.Argument.compressS2c = s2c;
			sHandshake.Argument.compressC2s = c2s;
			sHandshake.Argument.encryptType = p.Argument.encryptType;
			sHandshake.Send(p.getSender());
			((TcpSocket)p.getSender()).setOutputSecurityCodec(p.Argument.encryptType, outputKey, s2c);

			// 为了防止服务器在Handshake以后马上发送数据，
			// 导致未加密数据和加密数据一起到达Client，这种情况很难处理。
			// 这个本质上是协议相关的问题：就是前面一个协议的处理结果影响后面数据处理。
			// 所以增加CHandshakeDone协议，在Client进入加密以后发送给Server。

			return 0L;
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			//noinspection DataFlowIssue
			p.getSender().close(ex);
			return 0L;
		}
	}

	private long processSHandshake0(SHandshake0 p) {
		try {
			// 拒绝已加密连接上的重复握手，防止重装编解码链泄漏旧资源
			if (p.getSender() instanceof TcpSocket tcp && tcp.isSecurity())
				throw new IllegalStateException("re-handshake on secured connection");

			var options = getConfig().getHandshakeOptions();
			var clientEncryptType = options.getEncryptType();
			var serverEncryptType = p.Argument.encryptType;
			// 客户端要求加密时拒绝服务端推荐明文（含仅压缩场景），防止静默降级
			if (serverEncryptType == Constant.eEncryptTypeDisable
					&& clientEncryptType != Constant.eEncryptTypeDisable)
				throw new IllegalStateException(getName() + " server recommends plaintext (encryptType=Disable) but client "
						+ "EncryptType=" + clientEncryptType
						+ ", refusing silent downgrade (check client/server HandshakeOptions)");
			// 客户端要求 RsaAes（服务端认证）时拒绝降级为匿名 DH 或其他模式
			if (clientEncryptType == Constant.eEncryptTypeRsaAes
					&& serverEncryptType != Constant.eEncryptTypeRsaAes)
				throw new IllegalStateException(getName() + " server recommends encryptType=" + serverEncryptType
						+ " but client requires RsaAes, refusing authentication downgrade");
			if (serverEncryptType != Constant.eEncryptTypeDisable
					&& !options.isSupportedEncrypt(serverEncryptType))
				throw new IllegalStateException(getName() + " server recommends unsupported encryptType=" + serverEncryptType);
			if (serverEncryptType != Constant.eEncryptTypeDisable
					|| p.Argument.compressS2c != Constant.eCompressTypeDisable
					|| p.Argument.compressC2s != Constant.eCompressTypeDisable) {
				startHandshake(p.Argument, Objects.requireNonNull(p.getSender()));
			} else {
				CHandshakeDone.instance.Send(p.getSender());
				OnHandshakeDone(Objects.requireNonNull(p.getSender()));
			}
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			//noinspection DataFlowIssue
			p.getSender().close(ex);
		}
		return 0L;
	}

	private long processSHandshake(SHandshake p) {
		Context ctx = null;
		try {
			//noinspection DataFlowIssue
			ctx = dhContext.remove(p.getSender().getSessionId());
			if (ctx != null) {
				// 协商一致性检查：SHandshake回显的加密类型必须与CHandshake请求的一致，否则握手可能已被篡改，拒绝。
				if (p.Argument.encryptType != ctx.encryptType)
					throw new IllegalStateException("encryptType mismatch: " + p.Argument.encryptType
							+ " expect " + ctx.encryptType);

				byte[] inputKey = null;
				byte[] outputKey = null;
				switch (p.Argument.encryptType) {
				case Constant.eEncryptTypeAes: {
					byte[] material = Helper.computeDHKey(1,
							new BigInteger(p.Argument.encryptParam), (BigInteger)ctx.context).toByteArray();
					var remoteInet = p.getSender().getRemoteInet();

					byte[] key = remoteInet != null ? remoteInet.getAddress().getAddress() : ByteBuffer.Empty;
					logger.debug("{} remoteIp={}", p.getSender().getSessionId(), Arrays.toString(key));

					int half = material.length / 2;
					outputKey = Digest.hmacMd5(key, material, 0, half);
					inputKey = Digest.hmacMd5(key, material, half, material.length - half);
					break;
				}
				case Constant.eEncryptTypeAesNoSecureIp: {
					byte[] material = Helper.computeDHKey(1,
							new BigInteger(p.Argument.encryptParam), (BigInteger)ctx.context).toByteArray();
					int half = material.length / 2;
					outputKey = Arrays.copyOfRange(material, 0, half);
					inputKey = Arrays.copyOfRange(material, half, material.length);
					break;
				}
				case Constant.eEncryptTypeRsaAes:
					outputKey = (byte[])ctx.context;
					inputKey = Arrays.copyOfRange(outputKey, 32, 64);
					break;
				}
				((TcpSocket)p.getSender()).setOutputSecurityCodec(p.Argument.encryptType, outputKey, p.Argument.compressC2s);
				((TcpSocket)p.getSender()).setInputSecurityCodec(p.Argument.encryptType, inputKey, p.Argument.compressS2c);
				CHandshakeDone.instance.Send(p.getSender());
				((TcpSocket)Objects.requireNonNull(p.getSender())).submitAction(() -> OnHandshakeDone(Objects.requireNonNull(p.getSender()))); // 须在设置输入输出安全编解码器之后调用
				return 0;
			}
			p.getSender().close(new IllegalStateException("handshake lost context."));
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			//noinspection DataFlowIssue
			p.getSender().close(ex);
		} finally {
			if (null != ctx && null != ctx.timeoutTask)
				ctx.timeoutTask.cancel(false);
		}
		return 0;
	}

	private int clientCompress(int c) {
		// 客户端检查一下当前版本是否支持推荐的压缩算法。
		// 如果不支持则统一使用最老的。
		// 这样当服务器新增了压缩算法，并且推荐了新的，客户端可以兼容它。
		if (c == Constant.eCompressTypeDisable)
			return c; // 推荐关闭压缩就关闭
		var options = getConfig().getHandshakeOptions();
		if (options.isSupportedCompress(c))
			return c; // 支持的压缩，直接使用推荐的。
		return Constant.eCompressTypeMppc; // 使用最老的压缩。
	}

	protected final void startHandshake(BSHandshake0Argument arg, AsyncSocket so) {
		try {
			Object context;
			byte[] encryptParam;
			switch (arg.encryptType) {
			case Constant.eEncryptTypeAes:
			case Constant.eEncryptTypeAesNoSecureIp:
				var randomBigInt = Helper.makeDHRandom();
				context = randomBigInt;
				encryptParam = Helper.generateDHResponse(1, randomBigInt).toByteArray();
				break;
			case Constant.eEncryptTypeRsaAes:
				var rsaPubKey = getConfig().getHandshakeOptions().getRsaPubKey();
				if (rsaPubKey == null)
					throw new IllegalStateException("need RsaPubKey in ServiceConf");
				var random64 = Helper.makeRandValues(64); // inputIv + inputKey + outputIv + outputKey
				context = random64;
				encryptParam = Cert.encryptRsa(rsaPubKey, random64);
				break;
			default:
				context = BigInteger.ZERO;
				encryptParam = ByteBuffer.Empty;
				break;
			}

			var ctx = new Context(context, arg.encryptType);
			if (null != dhContext.putIfAbsent(so.getSessionId(), ctx)) {
				throw new IllegalStateException("handshake duplicate context for same session.");
			}

			var cHandShake = new CHandshake();
			// 默认加密压缩尽量都有服务器决定，不进行选择。
			cHandShake.Argument.encryptType = arg.encryptType;
			cHandShake.Argument.encryptParam = encryptParam;
			cHandShake.Argument.compressS2c = clientCompress(arg.compressS2c);
			cHandShake.Argument.compressC2s = clientCompress(arg.compressC2s);
			cHandShake.Send(so);

			ctx.timeoutTask = TaskSpec.ofAction(() -> {
				if (null != dhContext.remove(so.getSessionId())) {
					so.close(new Exception("Handshake Timeout"));
				}
			}).scheduleNow(5000);
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			so.close(ex);
		}
	}
}
