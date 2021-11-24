package Zeze.Services;

import Zeze.Net.*;
import Zeze.Services.Handshake.CHandshake;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.Helper;
import Zeze.*;
import java.util.*;

import Zeze.Services.Handshake.SHandshake;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.math.*;
import java.util.concurrent.ConcurrentHashMap;

public class HandshakeBase extends Service {
	private static final Logger logger = LogManager.getLogger(HandshakeBase.class);

	private final HashSet<Long> HandshakeProtocols = new HashSet<>();

	// For Client Only
	private final ConcurrentHashMap<Long, BigInteger> DHContext = new ConcurrentHashMap<>();

	public HandshakeBase(String name, Zeze.Config config) throws Throwable {
		super(name, config);
	}

	public HandshakeBase(String name, Application app) throws Throwable {
		super(name, app);
	}

	public final boolean IsHandshakeProtocol(long typeId) {
		return HandshakeProtocols.contains(typeId);
	}
	
	protected final void AddHandshakeServerFactoryHandle() {
		{
			var tmp = new Zeze.Services.Handshake.CHandshake();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(CHandshake::new
					, this::ProcessCHandshake
					, true));
		} 
		{
			var tmp = new Zeze.Services.Handshake.CHandshakeDone();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(CHandshakeDone::new
					, this::ProcessCHandshakeDone
					, true));
		}
	}
	
	private int ProcessCHandshakeDone(Protocol p) throws Throwable {
		OnHandshakeDone(p.getSender());
		return 0;
	}

	private int ProcessCHandshake(Protocol _p) throws Throwable {
		Zeze.Services.Handshake.CHandshake p = (Zeze.Services.Handshake.CHandshake)_p;
		int group = p.Argument.dh_group;
		if (!getConfig().getHandshakeOptions().getDhGroups().contains(group)) {
			p.getSender().Close(new RuntimeException("dhGroup Not Supported"));
			return 0;
		}
		
		BigInteger data = new BigInteger(p.Argument.dh_data);
		BigInteger rand = Helper.makeDHRandom();
		byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
		var localaddress = p.getSender().getSocket().getLocalAddress();
		byte[] key = getConfig().getHandshakeOptions().getSecureIp() != null
			? getConfig().getHandshakeOptions().getSecureIp() : localaddress.getAddress();
		logger.debug("{} localip={}", p.getSender().getSessionId(), Arrays.toString(key));
		int half = material.length / 2;

		byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		p.getSender().SetInputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getC2sNeedCompress(), null);

		byte[] response = Helper.generateDHResponse(group, rand).toByteArray();
		
		(new Zeze.Services.Handshake.SHandshake(response,
				getConfig().getHandshakeOptions().getS2cNeedCompress(),
				getConfig().getHandshakeOptions().getC2sNeedCompress())).Send(p.getSender());
		hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
		p.getSender().SetOutputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getS2cNeedCompress(), null);

		// 为了防止服务器在Handshake以后马上发送数据，
		// 导致未加密数据和加密数据一起到达Client，这种情况很难处理。
		// 这个本质上是协议相关的问题：就是前面一个协议的处理结果影响后面数据处理。
		// 所以增加CHandshakeDone协议，在Client进入加密以后发送给Server。
		// OnHandshakeDone(p.Sender);

		return 0;
	}

	protected final void AddHandshakeClientFactoryHandle() {
		var tmp = new Zeze.Services.Handshake.SHandshake();
		HandshakeProtocols.add(tmp.getTypeId());
		AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(SHandshake::new
				, this::ProcessSHandshake, true));
	}

	private int ProcessSHandshake(Protocol _p) throws Throwable {
		Zeze.Services.Handshake.SHandshake p = (Zeze.Services.Handshake.SHandshake)_p;
		var dhRandom = DHContext.get(p.getSender().getSessionId());
		if ( dhRandom != null ) {

			byte[] material = Helper.computeDHKey(getConfig().getHandshakeOptions().getDhGroup(), new BigInteger(p.Argument.dh_data), dhRandom).toByteArray();
			var remoteaddress = p.getSender().getSocket().getInetAddress();

			byte[] key = remoteaddress.getAddress();
			logger.debug("{} remoteip={}", p.getSender().getSessionId(), Arrays.toString(key));

			int half = material.length / 2;

			byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			p.getSender().SetOutputSecurityCodec(hmacMd5, p.Argument.c2sneedcompress, null);
			hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);

			DHContext.remove(p.getSender().getSessionId());
			p.getSender().SetInputSecurityCodec(hmacMd5, p.Argument.s2cneedcompress, ()->OnHandshakeDone(p.getSender()));
			(new Zeze.Services.Handshake.CHandshakeDone()).Send(p.getSender());
			return 0;
		}
		throw new RuntimeException("handshake lost context.");
	}

	protected final void StartHandshake(AsyncSocket so) {
		BigInteger dhRandom = Helper.makeDHRandom();

		if ( null != DHContext.putIfAbsent(so.getSessionId(), dhRandom)) {
			throw new RuntimeException("handshake duplicate context for same session.");
		}

		byte[] response = Helper.generateDHResponse(getConfig().getHandshakeOptions().getDhGroup(), dhRandom).toByteArray();
		(new Zeze.Services.Handshake.CHandshake(getConfig().getHandshakeOptions().getDhGroup(), response)).Send(so);
	}
}
