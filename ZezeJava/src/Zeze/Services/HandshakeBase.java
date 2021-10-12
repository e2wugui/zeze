package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Services.Handshake.Helper;
import Zeze.Transaction.*;
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
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] material = Handshake.Helper.computeDHKey(group, data, rand).ToByteArray();
		byte[] material = Helper.computeDHKey(group, data, rand).toByteArray();
		System.Net.IPAddress ipaddress = ((IPEndPoint)p.Sender.getSocket().LocalEndPoint).Address;
		//logger.Debug(ipaddress);
		if (ipaddress.IsIPv4MappedToIPv6) {
			ipaddress = ipaddress.MapToIPv4();
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = Config.HandshakeOptions.SecureIp != null ? Config.HandshakeOptions.SecureIp : ipaddress.GetAddressBytes();
		byte[] key = getConfig().getHandshakeOptions().getSecureIp() != null ? getConfig().getHandshakeOptions().getSecureIp() : ipaddress.GetAddressBytes();
		logger.Debug("{0} localip={1}", p.Sender.getSessionId(), BitConverter.toString(key));
		int half = material.length / 2;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		p.Sender.SetInputSecurityCodec(hmacMd5, getConfig().getHandshakeOptions().getC2sNeedCompress());
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] response = Handshake.Helper.generateDHResponse(group, rand).ToByteArray();
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
		TValue dhRandom;
		Zeze.Util.OutObject<TValue> tempOut_dhRandom = new Zeze.Util.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (DHContext.TryGetValue(p.Sender.getSessionId(), tempOut_dhRandom)) {
		dhRandom = tempOut_dhRandom.outArgValue;
			
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] material = Handshake.Helper.computeDHKey(Config.HandshakeOptions.DhGroup, new BigInteger(p.Argument.dh_data), dhRandom).ToByteArray();
			byte[] material = Helper.computeDHKey(getConfig().getHandshakeOptions().getDhGroup(), new BigInteger(p.Argument.dh_data), dhRandom).toByteArray();
			
			System.Net.IPAddress ipaddress = ((IPEndPoint)p.Sender.getSocket().RemoteEndPoint).Address;
			if (ipaddress.IsIPv4MappedToIPv6) {
				ipaddress = ipaddress.MapToIPv4();
			}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = ipaddress.GetAddressBytes();
			byte[] key = ipaddress.GetAddressBytes();
			logger.Debug("{0} remoteip={1}", p.Sender.getSessionId(), BitConverter.toString(key));

			int half = material.length / 2;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			p.Sender.SetOutputSecurityCodec(hmacMd5, p.Argument.c2sneedcompress);
			hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
			p.Sender.SetInputSecurityCodec(hmacMd5, p.Argument.s2cneedcompress);

			TValue _;
			Zeze.Util.OutObject<BigInteger> tempOut__ = new Zeze.Util.OutObject<BigInteger>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			DHContext.TryRemove(p.Sender.getSessionId(), tempOut__);
		_ = tempOut__.outArgValue;
			(new Zeze.Services.Handshake.CHandshakeDone()).Send(p.Sender);
			OnHandshakeDone(p.Sender);
			return 0;
		}
	else {
		dhRandom = tempOut_dhRandom.outArgValue;
	}
		throw new RuntimeException("handshake lost context.");
	}

	protected final void StartHandshake(AsyncSocket so) {
		BigInteger dhRandom = Helper.makeDHRandom();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if ( null != DHContext.putIfAbsent(so.getSessionId(), dhRandom)) {
			throw new RuntimeException("handshake duplicate context for same session.");
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] response = Handshake.Helper.generateDHResponse(Config.HandshakeOptions.DhGroup, dhRandom).ToByteArray();
		byte[] response = Helper.generateDHResponse(getConfig().getHandshakeOptions().getDhGroup(), dhRandom).toByteArray();
		(new Zeze.Services.Handshake.CHandshake(getConfig().getHandshakeOptions().getDhGroup(), response)).Send(so);
	}
}