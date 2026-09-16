package Zeze.Services;

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
import Zeze.Util.LongHashSet;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakeBase extends Service {
	private static final @NotNull Logger logger = LogManager.getLogger(HandshakeBase.class);

	private final LongHashSet handshakeProtocols = new LongHashSet();

	// 是否承担握手服务端角色（接受CHandshake）：构造期由addHandshakeServerFactoryHandle置位，
	// 必然早于start()里的checkAesSecureIp。作为一等语义判角色，不借handshakeProtocols派发
	// 簿记作预言——若未来出现start后才注册CHandshake的懒注册角色，簿记判据会静默漏告警。
	private boolean serverRole;

	static class Context {
		final @NotNull Object context;
		final int encryptType; // 本次握手请求使用的加密类型（CHandshake.Argument.encryptType），用于校验SHandshake回显一致
		@Nullable Future<?> timeoutTask;

		Context(@NotNull Object context, int encryptType) {
			this.context = context;
			this.encryptType = encryptType;
		}
	}

	// For Client Only
	private final LongConcurrentHashMap<Context> dhContext = new LongConcurrentHashMap<>();

	public HandshakeBase(@NotNull String name, @Nullable Config config) {
		super(name, config);
	}

	public HandshakeBase(@NotNull String name, @Nullable Application app) {
		super(name, app);
	}

	/**
	 * 连接数上限检查。
	 * 覆写 OnSocketAccept 的握手服务子类（HandshakeServer/HandshakeBoth/TokenServer 等）不再走
	 * Service.OnSocketAccept 的默认实现，会丢失其中的 maxConnections 检查（FND-S3-2）；
	 * 这些覆写点必须在第一行调用本方法恢复强制。超限时抛出 IllegalStateException，
	 * 由 accept 流程（TcpSocket 构造的 catch）关闭新连接。
	 */
	protected final void checkMaxConnections() {
		if (getSocketCount() >= getConfig().getMaxConnections())
			throw new IllegalStateException("too many connections");
	}

	/**
	 * FND7-23 加密门禁：EncryptType != Disable 时，未完成握手（加解密 codec 未装好，
	 * {@link TcpSocket#isSecurity()}==false，与 {@link TcpSocket#verifySecurity()} 同一判据）的连接
	 * 只允许处理握手协议本身（{@link #isHandshakeProtocol}），否则明文应用协议会被正常解码派发，
	 * 整个握手加密可被"不握手直连"绕过——此前框架唯一的强制点 verifySecurity() 只挂在
	 * processCHandshakeDone 上，由对端自愿发送。这里在输入侧逐帧预检：帧头 12 字节即可判定协议
	 * 类型，非握手协议立即抛异常断连（Selector 会 close）；不完整帧留待 Protocol.decode 继续接收
	 * 并做大小检查。握手完成后放行；EncryptType=Disable 的服务完全不经过本检查。
	 * 不用 OnHandshakeDone 标志判"完成"：客户端在 processSHandshake 装好 codec 之后、
	 * OnHandshakeDone 的 submitAction 执行之前，就可能收到对端握手后立即发送的已加密数据，
	 * 按标志门禁会误杀该合法时序（两个回调都在 selector 线程的 operates 队列里，事件顺序不保证）。
	 */
	@Override
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull ByteBuffer input) throws Exception {
		if (so instanceof TcpSocket tcp) {
			// haProxy 头必须先于帧扫描消费（对齐 Service 默认实现；已消费时 decodeHeader 立即返回true）。
			var haProxyHeader = tcp.getHaProxyHeader();
			if (haProxyHeader != null && !haProxyHeader.decodeHeader(input))
				return true; // 没有解析完header，看作成功。
			if (getConfig().getHandshakeOptions().getEncryptType() != Constant.eEncryptTypeDisable && !tcp.isSecurity()) {
				var bytes = input.Bytes;
				int readIndex = input.ReadIndex;
				while (input.WriteIndex - readIndex >= Protocol.HEADER_SIZE) {
					long typeId = Protocol.makeTypeId(ByteBuffer.ToInt(bytes, readIndex),
							ByteBuffer.ToInt(bytes, readIndex + 4));
					if (!isHandshakeProtocol(typeId))
						throw new IllegalStateException(getName()
								+ " reject plaintext protocol before handshake done: moduleId="
								+ Protocol.getModuleId(typeId) + " protocolId=" + Protocol.getProtocolId(typeId)
								+ " so=" + tcp);
					long frameEnd = (long)readIndex + Protocol.HEADER_SIZE
							+ (ByteBuffer.ToInt(bytes, readIndex + 8) & 0xffff_ffffL);
					if (frameEnd > input.WriteIndex)
						break; // 不完整帧：只做类型预检，大小合法性由 Protocol.decode 检查。
					readIndex = (int)frameEnd;
				}
			}
		}
		return super.OnSocketProcessInputBuffer(so, input);
	}

	/**
	 * Aes 模式会话密钥由连接地址参与派生——服务器取本端地址、客户端取对端地址
	 * （见 processCHandshake/processSHandshake）。直连时两侧地址一致；NAT/端口映射下服务器视角
	 * 是内网落地地址、客户端视角是公网拨入地址，两侧派生出不同密钥，该部署所有连接握手必然
	 * 失败并反复重连。此处启动时显著告警；修复：服务器显式配置 SecureIp 为客户端实际拨入的
	 * 地址（仅服务器侧读取），或改用 AesNoSecureIp/RsaAes。
	 */
	private void checkAesSecureIp() {
		var options = getConfig().getHandshakeOptions();
		if (options.getEncryptType() == Constant.eEncryptTypeAes && options.getSecureIp() == null) {
			// FND6-32：仅服务角色（HandshakeServer/HandshakeBoth）告警。客户端侧密钥派生从不
			// 读SecureIp（全仓唯一读取点就是本告警），纯客户端角色（HandshakeClient）启动即
			// WARN属必然误报——协商值由服务器决定，服务器配SecureIp/直连时客户端完全正常。
			// serverRole构造期确定（仅服务角色构造器注册CHandshake工厂），无懒注册时序漏判。
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
		return handshakeProtocols.contains(typeId);
	}

	protected final void addHandshakeServerFactoryHandle() {
		serverRole = true;
		handshakeProtocols.add(CHandshake.TypeId_);
		AddFactoryHandle(CHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshake::new, this::processCHandshake, TransactionLevel.None, DispatchMode.Direct));
		handshakeProtocols.add(CHandshakeDone.TypeId_);
		AddFactoryHandle(CHandshakeDone.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshakeDone::new, this::processCHandshakeDone, TransactionLevel.None, DispatchMode.Direct));
		handshakeProtocols.add(KeepAlive.TypeId_);
		if (!getFactorys().containsKey(KeepAlive.TypeId_)) {
			AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
					HandshakeBase::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));
		}
	}

	private static long processKeepAliveRequest(@NotNull KeepAlive r) {
		r.SendResult();
		return 0L;
	}

	private long processCHandshakeDone(@NotNull CHandshakeDone p) {
		try {
			((TcpSocket)p.getSender()).verifySecurity();
			OnHandshakeDone(p.getSender());
			return 0L;
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
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

	private long processCHandshake(@NotNull CHandshake p) {
		try {
			// 协商一致性检查：客户端上报的加密类型必须与服务器配置（即SHandshake0发出的推荐值）一致，
			// 否则握手可能已被篡改（如RsaAes被降级成匿名DH），拒绝（FND-S3-1 部分缓解）。
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
				/*
				if (!getConfig().getHandshakeOptions().getDhGroups().contains(group)) {
					p.getSender().close(new UnsupportedOperationException("dhGroup Not Supported"));
					return 0L;
				}
				*/

				BigInteger data = new BigInteger(p.Argument.encryptParam);
				BigInteger rand = Helper.makeDHRandom();
				byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
				var localInet = ((TcpSocket)p.getSender()).getLocalInet();
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
			// OnHandshakeDone(p.Sender);

			return 0L;
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			p.getSender().close(ex);
			return 0L;
		}
	}

	protected final void addHandshakeClientFactoryHandle() {
		handshakeProtocols.add(SHandshake0.TypeId_);
		AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake0::new, this::processSHandshake0, TransactionLevel.None, DispatchMode.Direct));
		handshakeProtocols.add(SHandshake.TypeId_);
		AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake::new, this::processSHandshake, TransactionLevel.None, DispatchMode.Direct));
		handshakeProtocols.add(KeepAlive.TypeId_);
		if (!getFactorys().containsKey(KeepAlive.TypeId_)) {
			AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
					HandshakeBase::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));
		}
	}

	private long processSHandshake0(@NotNull SHandshake0 p) {
		try {
			// 复审R2（FND7-S2②）+R3收窄：服务端推荐的加密类型为Disable（明文）而客户端自身配置了
			// 加密诉求（EncryptType!=Disable）时不得静默接受——否则"客户端要求加密"的配置被无声降级
			// 为明文会话。检查必须在分支之前：仅压缩推荐（encryptType=Disable+compress非Disable）也走
			// startHandshake路径，客户端只会回显服务端推荐（startHandshake不读自身EncryptType），
			// 降级照样发生且原全Disable分支的检查永远不触发。镜像服务端processCHandshake的回显一致性
			// 校验（FND-S3-1：服务端拒绝encryptType不一致的CHandshake）：客户端拒绝被降级的推荐，
			// 断连给出显式配置错误。
			if (p.Argument.encryptType == Constant.eEncryptTypeDisable
					&& getConfig().getHandshakeOptions().getEncryptType() != Constant.eEncryptTypeDisable)
				throw new IllegalStateException(getName() + " server recommends plaintext (encryptType=Disable) but client "
						+ "EncryptType=" + getConfig().getHandshakeOptions().getEncryptType()
						+ ", refusing silent downgrade (check client/server HandshakeOptions)");
			if (p.Argument.encryptType != Constant.eEncryptTypeDisable
					|| p.Argument.compressS2c != Constant.eCompressTypeDisable
					|| p.Argument.compressC2s != Constant.eCompressTypeDisable) {
				startHandshake(p.Argument, p.getSender());
			} else {
				CHandshakeDone.instance.Send(p.getSender());
				OnHandshakeDone(p.getSender());
			}
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			p.getSender().close(ex);
		}
		return 0L;
	}

	private long processSHandshake(@NotNull SHandshake p) {
		Context ctx = null;
		try {
			ctx = dhContext.remove(p.getSender().getSessionId());
			if (ctx != null) {
				// 协商一致性检查：SHandshake回显的加密类型必须与CHandshake请求的一致，否则握手可能已被篡改，拒绝（FND-S3-1 部分缓解）。
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
				((TcpSocket)p.getSender()).submitAction(() -> OnHandshakeDone(p.getSender())); // must after SetInputSecurityCodec and SetOutputSecurityCodec
				return 0;
			}
			p.getSender().close(new IllegalStateException("handshake lost context."));
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
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

	protected final void startHandshake(@NotNull BSHandshake0Argument arg, @NotNull AsyncSocket so) {
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
