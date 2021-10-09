package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.math.*;

	private int ProcessCHandshakeDone(Protocol p) {
		OnHandshakeDone(p.Sender);
		return 0;
	}

	private int ProcessCHandshake(Protocol _p) {
		Zeze.Services.Handshake.CHandshake p = (Zeze.Services.Handshake.CHandshake)_p;
		int group = p.getArgument().dh_group;
		if (false == getConfig().HandshakeOptions.getDhGroups().contains(group)) {
			p.getSender().Close(new RuntimeException("dhGroup Not Supported"));
			return 0;
		}
		Array.Reverse(p.getArgument().dh_data);
		BigInteger data = new BigInteger(p.getArgument().dh_data);
		BigInteger rand = Handshake.Helper.makeDHRandom();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] material = Handshake.Helper.computeDHKey(group, data, rand).ToByteArray();
		byte[] material = Handshake.Helper.computeDHKey(group, data, rand).toByteArray();
		Array.Reverse(material);
		System.Net.IPAddress ipaddress = ((IPEndPoint)p.getSender().getSocket().LocalEndPoint).Address;
		//logger.Debug(ipaddress);
		if (ipaddress.IsIPv4MappedToIPv6) {
			ipaddress = ipaddress.MapToIPv4();
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = Config.HandshakeOptions.SecureIp != null ? Config.HandshakeOptions.SecureIp : ipaddress.GetAddressBytes();
		byte[] key = getConfig().HandshakeOptions.getSecureIp() != null ? getConfig().HandshakeOptions.getSecureIp() : ipaddress.GetAddressBytes();
		logger.Debug("{0} localip={1}", p.getSender().getSessionId(), BitConverter.toString(key));
		int half = material.length / 2;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
		p.getSender().SetInputSecurityCodec(hmacMd5, getConfig().HandshakeOptions.getC2sNeedCompress());
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] response = Handshake.Helper.generateDHResponse(group, rand).ToByteArray();
		byte[] response = Handshake.Helper.generateDHResponse(group, rand).toByteArray();
		Array.Reverse(response);
		(new Zeze.Services.Handshake.SHandshake(response, getConfig().HandshakeOptions.getS2cNeedCompress(), getConfig().HandshakeOptions.getC2sNeedCompress())).Send(p.getSender());
		hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
		p.getSender().SetOutputSecurityCodec(hmacMd5, getConfig().HandshakeOptions.getS2cNeedCompress());

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
		AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Zeze.Services.Handshake.SHandshake(), Handle = ProcessSHandshake, NoProcedure = true});
	}

	private int ProcessSHandshake(Protocol _p) {
		Zeze.Services.Handshake.SHandshake p = (Zeze.Services.Handshake.SHandshake)_p;
		TValue dhRandom;
		tangible.OutObject<TValue> tempOut_dhRandom = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (DHContext.TryGetValue(p.getSender().getSessionId(), tempOut_dhRandom)) {
		dhRandom = tempOut_dhRandom.outArgValue;
			Array.Reverse(p.getArgument().dh_data);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] material = Handshake.Helper.computeDHKey(Config.HandshakeOptions.DhGroup, new BigInteger(p.Argument.dh_data), dhRandom).ToByteArray();
			byte[] material = Handshake.Helper.computeDHKey(getConfig().HandshakeOptions.getDhGroup(), new BigInteger(p.getArgument().dh_data), dhRandom).toByteArray();
			Array.Reverse(material);
			System.Net.IPAddress ipaddress = ((IPEndPoint)p.getSender().getSocket().RemoteEndPoint).Address;
			if (ipaddress.IsIPv4MappedToIPv6) {
				ipaddress = ipaddress.MapToIPv4();
			}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = ipaddress.GetAddressBytes();
			byte[] key = ipaddress.GetAddressBytes();
			logger.Debug("{0} remoteip={1}", p.getSender().getSessionId(), BitConverter.toString(key));

			int half = material.length / 2;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
			p.getSender().SetOutputSecurityCodec(hmacMd5, p.getArgument().c2sneedcompress);
			hmacMd5 = Digest.HmacMd5(key, material, half, material.length - half);
			p.getSender().SetInputSecurityCodec(hmacMd5, p.getArgument().s2cneedcompress);

			TValue _;
			tangible.OutObject<BigInteger> tempOut__ = new tangible.OutObject<BigInteger>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			DHContext.TryRemove(p.getSender().getSessionId(), tempOut__);
		_ = tempOut__.outArgValue;
			(new Zeze.Services.Handshake.CHandshakeDone()).Send(p.getSender());
			OnHandshakeDone(p.getSender());
			return 0;
		}
	else {
		dhRandom = tempOut_dhRandom.outArgValue;
	}
		throw new RuntimeException("handshake lost context.");
	}

	protected final void StartHandshake(AsyncSocket so) {
		BigInteger dhRandom = Handshake.Helper.makeDHRandom();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!DHContext.TryAdd(so.getSessionId(), dhRandom)) {
			throw new RuntimeException("handshake duplicate context for same session.");
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] response = Handshake.Helper.generateDHResponse(Config.HandshakeOptions.DhGroup, dhRandom).ToByteArray();
		byte[] response = Handshake.Helper.generateDHResponse(getConfig().HandshakeOptions.getDhGroup(), dhRandom).toByteArray();
		Array.Reverse(response);
		(new Zeze.Services.Handshake.CHandshake(getConfig().HandshakeOptions.getDhGroup(), response)).Send(so);
	}
}

public class HandshakeServer extends HandshakeBase {
	public HandshakeServer(String name, Config config) {
		super(name, config);
		AddHandshakeServerFactoryHandle();
	}

	public HandshakeServer(String name, Application app) {
		super(name, app);
		AddHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		SocketMap.TryAdd(so.getSessionId(), so);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// 防止Client不进入加密，直接发送用户协议。
		if (false == IsHandshakeProtocol(p.getTypeId())) {
			p.Sender.VerifySecurity();
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}