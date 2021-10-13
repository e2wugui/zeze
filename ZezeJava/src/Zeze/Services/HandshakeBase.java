package Zeze.Services;

import Zeze.Net.*;
import Zeze.Services.Handshake.Helper;
import Zeze.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.math.*;

public class HandshakeBase extends Service {
	private static final Logger logger = LogManager.getLogger(HandshakeBase.class);

	private HashSet<Integer> HandshakeProtocols = new HashSet<Integer>();

	// For Client Only
	private java.util.concurrent.ConcurrentHashMap<Long, BigInteger> DHContext = new java.util.concurrent.ConcurrentHashMap<Long, BigInteger>();

	public HandshakeBase(String name, Config config) {
		super(name, config);
	}

	public HandshakeBase(String name, Application app) {
		super(name, app);
	}

	public final boolean IsHandshakeProtocol(int typeId) {
		return HandshakeProtocols.contains(typeId);
	}
	
	protected final void AddHandshakeServerFactoryHandle() {
		{
			var tmp = new Zeze.Services.Handshake.CHandshake();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(() -> new Zeze.Services.Handshake.CHandshake()
					, this::ProcessCHandshake
					, true));
		} 
		{
			var tmp = new Zeze.Services.Handshake.CHandshakeDone();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(() -> new Zeze.Services.Handshake.CHandshakeDone()
					, this::ProcessCHandshakeDone
					, true));
		}
	}
	
	private int ProcessCHandshakeDone(Protocol p) {
		OnHandshakeDone(p.Sender);
		return 0;
	}

	private int ProcessCHandshake(Protocol _p) {
		Zeze.Services.Handshake.CHandshake p = (Zeze.Services.Handshake.CHandshake)_p;
		int group = p.Argument.dh_group;
		if (false == getConfig().getHandshakeOptions().getDhGroups().contains(group)) {
			p.Sender.Close(new RuntimeException("dhGroup Not Supported"));
			return 0;
		}
		
		BigInteger data = new BigInteger(p.Argument.dh_data);
		BigInteger rand = Helper.makeDHRandom();
		byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
		var localaddress = p.Sender.getSocket().getLocalAddress();
		byte[] key = getConfig().getHandshakeOptions().getSecureIp() != null
			? getConfig().getHandshakeOptions().getSecureIp() : localaddress.getAddress();
		logger.debug("{} localip={}", p.Sender.getSessionId(), Zeze.Util.BitConverter.toString(key));
		int half = material.length / 2;

		byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		p.Sender.SetInputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getC2sNeedCompress());

		byte[] response = Helper.generateDHResponse(group, rand).toByteArray();
		
		(new Zeze.Services.Handshake.SHandshake(response, getConfig().getHandshakeOptions().getS2cNeedCompress(), getConfig().getHandshakeOptions().getC2sNeedCompress())).Send(p.Sender);
		hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
		p.Sender.SetOutputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getS2cNeedCompress());

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
		AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(() -> new Zeze.Services.Handshake.SHandshake()
				, this::ProcessSHandshake, true));
	}

	private int ProcessSHandshake(Protocol _p) {
		Zeze.Services.Handshake.SHandshake p = (Zeze.Services.Handshake.SHandshake)_p;
		var dhRandom = DHContext.get(p.Sender.getSessionId());
		if ( dhRandom != null ) {

			byte[] material = Helper.computeDHKey(getConfig().getHandshakeOptions().getDhGroup(), new BigInteger(p.Argument.dh_data), dhRandom).toByteArray();
			var remoteaddress = p.Sender.getSocket().getInetAddress();

			byte[] key = remoteaddress.getAddress();
			logger.debug("{} remoteip={}", p.Sender.getSessionId(), Zeze.Util.BitConverter.toString(key));

			int half = material.length / 2;

			byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			p.Sender.SetOutputSecurityCodec(hmacMd5, p.Argument.c2sneedcompress);
			hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
			p.Sender.SetInputSecurityCodec(hmacMd5, p.Argument.s2cneedcompress);

			DHContext.remove(p.Sender.getSessionId());		
			(new Zeze.Services.Handshake.CHandshakeDone()).Send(p.Sender);
			OnHandshakeDone(p.Sender);
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
