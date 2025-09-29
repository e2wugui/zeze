package Zeze.Services;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Digest;
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
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakeBase extends Service {
	private static final @NotNull Logger logger = LogManager.getLogger(HandshakeBase.class);

	private final LongHashSet handshakeProtocols = new LongHashSet();

	static class Context {
		final @NotNull Object context;
		@Nullable Future<?> timeoutTask;

		Context(@NotNull Object context) {
			this.context = context;
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

	@Override
	public boolean isHandshakeProtocol(long typeId) {
		return handshakeProtocols.contains(typeId);
	}

	protected final void addHandshakeServerFactoryHandle() {
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

	private long processCHandshakeDone(@NotNull CHandshakeDone p) throws Exception {
		((TcpSocket)p.getSender()).verifySecurity();
		OnHandshakeDone(p.getSender());
		return 0L;
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

			var ctx = new Context(context);
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

			ctx.timeoutTask = Task.scheduleUnsafe(5000, () -> {
				if (null != dhContext.remove(so.getSessionId())) {
					so.close(new Exception("Handshake Timeout"));
				}
			});
		} catch (Throwable ex) { // 这是普通协议，而Service.Dispatch可能会被重载成忽略协议处理错误，但是这个握手错误不能忽略。
			so.close(ex);
		}
	}
}
