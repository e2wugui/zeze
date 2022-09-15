package Zeze.Services;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Digest;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.CHandshake;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.Helper;
import Zeze.Services.Handshake.SHandshake;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HandshakeBase extends Service {
	private static final Logger logger = LogManager.getLogger(HandshakeBase.class);

	private final LongHashSet HandshakeProtocols = new LongHashSet();

	static class Context {
		final BigInteger dhRandom;
		Future<?> timeoutTask;

		Context(BigInteger random) {
			dhRandom = random;
		}
	}

	// For Client Only
	private final LongConcurrentHashMap<Context> DHContext = new LongConcurrentHashMap<>();

	public HandshakeBase(String name, Zeze.Config config) throws Throwable {
		super(name, config);
	}

	public HandshakeBase(String name, Application app) throws Throwable {
		super(name, app);
	}

	@Override
	public boolean isHandshakeProtocol(long typeId) {
		return HandshakeProtocols.contains(typeId);
	}

	protected final void AddHandshakeServerFactoryHandle() {
		HandshakeProtocols.add(CHandshake.TypeId_);
		AddFactoryHandle(CHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshake::new, this::ProcessCHandshake, TransactionLevel.None, DispatchMode.Normal));
		HandshakeProtocols.add(CHandshakeDone.TypeId_);
		AddFactoryHandle(CHandshakeDone.TypeId_, new Service.ProtocolFactoryHandle<>(
				CHandshakeDone::new, this::ProcessCHandshakeDone, TransactionLevel.None, DispatchMode.Normal));
	}

	private long ProcessCHandshakeDone(CHandshakeDone p) throws Throwable {
		p.getSender().VerifySecurity();
		OnHandshakeDone(p.getSender());
		return 0L;
	}

	private long ProcessCHandshake(CHandshake p) {
		try {
			int group = p.Argument.dh_group;
			if (!getConfig().getHandshakeOptions().getDhGroups().contains(group)) {
				p.getSender().close(new UnsupportedOperationException("dhGroup Not Supported"));
				return 0L;
			}

			BigInteger data = new BigInteger(p.Argument.dh_data);
			BigInteger rand = Helper.makeDHRandom();
			byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
			var localAddress = p.getSender().getLocalInetAddress();
			byte[] key = getConfig().getHandshakeOptions().getSecureIp() != null
					? getConfig().getHandshakeOptions().getSecureIp()
					: (localAddress != null ? localAddress.getAddress() : ByteBuffer.Empty);
			logger.debug("{} localIp={}", p.getSender().getSessionId(), Arrays.toString(key));
			int half = material.length / 2;

			byte[] hmacMd5 = Digest.hmacMd5(key, material, 0, half);
			p.getSender().SetInputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getC2sNeedCompress());

			byte[] response = Helper.generateDHResponse(group, rand).toByteArray();

			(new Zeze.Services.Handshake.SHandshake(response,
					getConfig().getHandshakeOptions().getS2cNeedCompress(),
					getConfig().getHandshakeOptions().getC2sNeedCompress())).Send(p.getSender());
			hmacMd5 = Digest.hmacMd5(key, material, half, material.length - half);
			p.getSender().SetOutputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getS2cNeedCompress());

			// 为了防止服务器在Handshake以后马上发送数据，
			// 导致未加密数据和加密数据一起到达Client，这种情况很难处理。
			// 这个本质上是协议相关的问题：就是前面一个协议的处理结果影响后面数据处理。
			// 所以增加CHandshakeDone协议，在Client进入加密以后发送给Server。
			// OnHandshakeDone(p.Sender);

			return 0L;
		} catch (Throwable ex) {
			p.getSender().close(ex);
			return 0L;
		}
	}

	protected final void AddHandshakeClientFactoryHandle() {
		HandshakeProtocols.add(SHandshake.TypeId_);
		AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake::new, this::ProcessSHandshake, TransactionLevel.None, DispatchMode.Normal));
		HandshakeProtocols.add(SHandshake0.TypeId_);
		AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(
				SHandshake0::new, this::ProcessSHandshake0, TransactionLevel.None, DispatchMode.Normal));
	}

	private long ProcessSHandshake0(SHandshake0 p) {
		try {
			if (p.Argument.EnableEncrypt) {
				StartHandshake(p.getSender());
			} else {
				new CHandshakeDone().Send(p.getSender());
				OnHandshakeDone(p.getSender());
			}
		} catch (Throwable ex) {
			p.getSender().close(ex);
		}
		return 0L;
	}

	private long ProcessSHandshake(SHandshake p) {
		Context ctx = null;
		try {
			ctx = DHContext.remove(p.getSender().getSessionId());
			if (ctx != null) {
				byte[] material = Helper.computeDHKey(getConfig().getHandshakeOptions().getDhGroup(),
						new BigInteger(p.Argument.dh_data), ctx.dhRandom).toByteArray();
				var remoteAddress = p.getSender().getRemoteInetAddress();

				byte[] key = remoteAddress != null ? remoteAddress.getAddress() : ByteBuffer.Empty;
				logger.debug("{} remoteIp={}", p.getSender().getSessionId(), Arrays.toString(key));

				int half = material.length / 2;

				byte[] hmacMd5 = Digest.hmacMd5(key, material, 0, half);
				p.getSender().SetOutputSecurityCodec(hmacMd5, p.Argument.c2sNeedCompress);
				hmacMd5 = Digest.hmacMd5(key, material, half, material.length - half);

				p.getSender().SetInputSecurityCodec(hmacMd5, p.Argument.s2cNeedCompress);
				(new Zeze.Services.Handshake.CHandshakeDone()).Send(p.getSender());
				p.getSender().SubmitAction(() -> OnHandshakeDone(p.getSender())); // must after SetInputSecurityCodec and SetOutputSecurityCodec
				return 0;
			}
			p.getSender().close(new IllegalStateException("handshake lost context."));
		} catch (Throwable ex) {
			p.getSender().close(ex);
		} finally {
			if (null != ctx && null != ctx.timeoutTask)
				ctx.timeoutTask.cancel(false);
		}
		return 0;
	}

	protected final void StartHandshake(AsyncSocket so) {
		try {
			var ctx = new Context(Helper.makeDHRandom());
			if (null != DHContext.putIfAbsent(so.getSessionId(), ctx)) {
				throw new IllegalStateException("handshake duplicate context for same session.");
			}

			byte[] response = Helper.generateDHResponse(getConfig().getHandshakeOptions().getDhGroup(), ctx.dhRandom).toByteArray();
			(new Zeze.Services.Handshake.CHandshake(getConfig().getHandshakeOptions().getDhGroup(), response)).Send(so);
			ctx.timeoutTask = Zeze.Util.Task.scheduleUnsafe(5000, () -> {
				if (null != DHContext.remove(so.getSessionId())) {
					so.close(new Exception("Handshake Timeout"));
				}
			});
		} catch (Throwable ex) {
			so.close(ex);
		}
	}
}
