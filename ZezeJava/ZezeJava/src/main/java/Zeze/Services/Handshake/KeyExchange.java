package Zeze.Services.Handshake;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Decrypt2;
import Zeze.Net.Digest;
import Zeze.Net.Encrypt2;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Cert;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TCP连接成功后,主动连接方(客户端)先发密钥交换请求(KeyExchange)给被动连接方(服务器). 收到回复后,后续通信用双方各自随机生成的key做双向对称加密(己方生成的key用于对方加密发送,己方接收解密)
// 客户端 === KeyExchange(明文请求) ==> 服务器 (客户端用服务器公钥加密客户端生成的key给服务器,可选提供客户端公钥)
// 客户端 <== KeyExchange(明文回复) === 服务器 (服务器用服务器私钥解密得到客户端的key,再生成服务器key,二者做异或回复给客户端,或者验证客户端公钥并用客户端公钥加密服务器key回给客户端,两种方法都能让客户端得到服务器key)
// 客户端 === 其它协议(使用服务器生成的key加密) ==> 服务器 (密钥交换后,需要由客户端先发起协议,方便客户端确定何时开始解密)
// 客户端 <== 其它协议(使用客户端生成的key加密) === 服务器
public final class KeyExchange extends Rpc<KeyExchange.Arg, KeyExchange.Res> {
	public static final int ModuleId = 0;
	public static final int ProtocolId = Bean.hash32(KeyExchange.class.getName()); // 571437512 0x220F71C8
	public static final long TypeId = Protocol.makeTypeId(ModuleId, ProtocolId); // 571437512 [00 00 00 00 C8 71 0F 22]
	public static final BigInteger pubKeyE = BigInteger.valueOf(65537); // RSA公钥中的参数E,固定值

	static {
		register(TypeId, KeyExchange.class);
	}

	public static final class Arg implements Serializable {
		public int version; // 版本. 目前只定义初始版本:0, 即固定使用RSA-2048(exponent固定为65537)作为非对称加密,AES-128(CFB模式)作为对称加密
		public byte[] clientPubKey; // 本地客户端公钥. RSA公钥中的N(modulus)以大端序列化成byte数组(最高位的字节不能为0). 用于对方验证自己的身份,可以为空(不验证,仅单向非对称加密)
		public byte[] serverPubKeyMd5; // 对方服务器公钥的MD5. 如上方法序列化后再做MD5, 用于对方选取所用的私钥
		public byte[] encIvKey; // 随机生成对称加密的iv和key各16字节拼接成32字节,以"RSA/ECB/PKCS1Padding"模式加密

		@Override
		public int preAllocSize() {
			return 534; // 1 + 2+256 + 1+16 + 2+256
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBytes(clientPubKey != null ? clientPubKey : ByteBuffer.Empty);
			bb.WriteBytes(serverPubKeyMd5 != null ? serverPubKeyMd5 : ByteBuffer.Empty);
			bb.WriteBytes(encIvKey != null ? encIvKey : ByteBuffer.Empty);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			version = bb.ReadInt();
			if (version != 0)
				throw new UnsupportedOperationException("version = " + version);
			clientPubKey = bb.ReadBytes();
			serverPubKeyMd5 = bb.ReadBytes();
			encIvKey = bb.ReadBytes();
		}
	}

	public static final class Res implements Serializable {
		// resultCode=0表示成功,其它见下方枚举定义
		public static final int ErrorUnknownClientPubKey = 1; // 未知或非法的客户端公钥(如果clientPubKey为空,则表示必须验证客户端公钥)
		public static final int ErrorUnknownServerPubKey = 2; // 未知或非法的服务器公钥
		public static final int ErrorDecryptFailed = 3; // 解密encIvKey失败

		public int version; // 版本. 同Arg的version
		public byte[] encIvKey; // 如果clientPubKey为空,表示双方随机生成的对称加密的iv和key的异或结果; 否则类似Arg中的encIvKey,给客户端加密发送服务器随机生成的iv和key

		@Override
		public int preAllocSize() {
			return 259; // 1 + 2+256
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBytes(encIvKey != null ? encIvKey : ByteBuffer.Empty);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			version = bb.ReadInt();
			if (version != 0)
				throw new UnsupportedOperationException("version = " + version);
			encIvKey = bb.ReadBytes();
		}
	}

	private byte[] clientIvKey; // 仅客户端用,在服务器处理时客户端暂存自己生成的iv和key

	public KeyExchange() {
		Argument = new Arg();
		Result = new Res();
	}

	public KeyExchange(byte @NotNull [] serverPubKey) {
		this(serverPubKey, null);
	}

	public KeyExchange(byte @NotNull [] serverPubKey, byte @Nullable [] clientPubKey) {
		this();
		Argument.clientPubKey = clientPubKey;
		Argument.serverPubKeyMd5 = getPubKeyMd5(serverPubKey);
		Argument.encIvKey = encryptRsa(serverPubKey, clientIvKey = genIvKey());
	}

	public static byte @NotNull [] getPubKeyMd5(byte @NotNull [] pubKey) {
		int i = 0;
		for (; i < pubKey.length; i++) {
			if (pubKey[i] != 0) // 跳过前面的字节0
				break;
		}
		return Digest.md5(pubKey, i, pubKey.length - i);
	}

	public static byte @NotNull [] encryptRsa(byte @NotNull [] pubKeyN, byte @NotNull [] data) {
		try {
			var rsaPubKey = Cert.loadRsaPublicKey(new BigInteger(1, pubKeyN), pubKeyE);
			return Cert.encryptRsa(rsaPubKey, data);
		} catch (GeneralSecurityException e) {
			Task.forceThrow(e);
			//noinspection UnreachableCode
			return ByteBuffer.Empty; // never run here
		}
	}

	public static byte @NotNull [] genIvKey() {
		byte[] ivKey = new byte[32];
		new SecureRandom().nextBytes(ivKey);
		return ivKey;
	}

	public boolean send(@NotNull AsyncSocket so) {
		return send(so, null);
	}

	public boolean send(@NotNull AsyncSocket so, @Nullable PrivateKey clientPriKey) {
		if ((Argument.clientPubKey == null) != (clientPriKey == null)) // 客户端公私钥必须同时提供
			throw new IllegalArgumentException();
		return Send(so, r -> {
			byte[] serverIvKey;
			int serverIvKeyLen;
			if (clientPriKey != null) {
				serverIvKey = Cert.decryptRsa(clientPriKey, Result.encIvKey);
				if (serverIvKey.length != 32)
					throw new IllegalStateException("ErrorDecryptFailed"); // 不该出现的意外情况,估计只能断开连接了
			} else {
				serverIvKey = Result.encIvKey;
				serverIvKeyLen = serverIvKey.length;
				if (serverIvKeyLen != 32)
					throw new IllegalStateException("ErrorDecryptFailed"); // 不该出现的意外情况,估计只能断开连接了
				for (int i = 0; i < 32; i++)
					serverIvKey[i] ^= clientIvKey[i];
			}
			byte[] serverIv = Arrays.copyOfRange(serverIvKey, 0, 16);
			byte[] serverKey = Arrays.copyOfRange(serverIvKey, 16, 32);
			r.getSender().setOutputSecurityCodec((__, outBuf) -> new Encrypt2(outBuf, serverKey, serverIv));

			byte[] clientIv = Arrays.copyOfRange(clientIvKey, 0, 16);
			byte[] clientKey = Arrays.copyOfRange(clientIvKey, 16, 32);
			r.getSender().setInputSecurityCodec((__, inBuf) -> new Decrypt2(inBuf, clientKey, clientIv));
			return 0;
		});
	}

	public long processKeyExchangeRequest(@NotNull PrivateKey priKey, byte @Nullable [] pubKeyMd5) {
		if (!Arrays.equals(Argument.serverPubKeyMd5, pubKeyMd5)) {
			trySendResultCode(Res.ErrorUnknownServerPubKey);
			return 0;
		}
		byte[] clientIvKey;
		try {
			clientIvKey = Cert.decryptRsa(priKey, Argument.encIvKey);
		} catch (GeneralSecurityException e) {
			Task.forceThrow(e);
			return 0; // never run here
		}
		if (clientIvKey.length != 32) {
			trySendResultCode(Res.ErrorDecryptFailed);
			return 0;
		}

		byte[] serverIvKey = genIvKey();
		byte[] serverIv = Arrays.copyOfRange(serverIvKey, 0, 16);
		byte[] serverKey = Arrays.copyOfRange(serverIvKey, 16, 32);
		getSender().setInputSecurityCodec((__, inBuf) -> new Decrypt2(inBuf, serverKey, serverIv));

		if (Argument.clientPubKey.length > 0) {
			//NOTE: 这里可以先认证一下客户端公钥是否合法,不合法就回复Res.ErrorUnknownClientPubKey
			Result.encIvKey = encryptRsa(Argument.clientPubKey, serverIvKey);
		} else {
			for (int i = 0; i < 32; i++)
				serverIvKey[i] ^= clientIvKey[i];
			Result.encIvKey = serverIvKey;
		}
		trySendResultCode(0);

		byte[] clientIv = Arrays.copyOfRange(clientIvKey, 0, 16);
		byte[] clientKey = Arrays.copyOfRange(clientIvKey, 16, 32);
		getSender().setOutputSecurityCodec((__, outBuf) -> new Encrypt2(outBuf, clientKey, clientIv));
		return 0;
	}

	public static void addHandler(@NotNull Service service, @NotNull PrivateKey serverPriKey) {
		byte[] pubKeyMd5 = getPubKeyMd5(((RSAKey)serverPriKey).getModulus().toByteArray());
		if (!service.getFactorys().containsKey(KeyExchange.TypeId)) {
			service.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
					r -> r.processKeyExchangeRequest(serverPriKey, pubKeyMd5),
					TransactionLevel.None, DispatchMode.Direct));
		}
	}

	@Override
	public int getModuleId() {
		return ModuleId;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId;
	}

	@Override
	public long getTypeId() {
		return TypeId;
	}

	public static void main(String[] args) throws GeneralSecurityException {
		// 生成RSA的公私钥
		long t = System.nanoTime();
		var keyPair = Cert.generateRsaKeyPair();
		System.out.println("generateRsaKeyPair: " + (System.nanoTime() - t) / 1_000_000 + " ms");

		// 获取RSA私钥并测试序列化/反序列化
		var priKey = keyPair.getPrivate();
		byte[] priKeyData = priKey.getEncoded();
		var priKey2 = Cert.loadRsaPrivateKey(priKeyData);
		System.out.println("check priKey encoded: " + priKey.equals(priKey2));

		// 获取RSA公钥并测试序列化/反序列化
		var pubKey = keyPair.getPublic();
		byte[] pubKeyData = pubKey.getEncoded();
		var pubKey2 = Cert.loadRsaPublicKey(pubKeyData);
		System.out.println("check pubKey encoded: " + pubKey.equals(pubKey2));

		// 获取RSA公钥中的参数N(modulus)
		byte[] pubKeyN = ((RSAKey)pubKey).getModulus().toByteArray();
		byte[] priKeyN = ((RSAKey)priKey).getModulus().toByteArray();
		System.out.println("pubKeyN = [" + pubKeyN.length + "] " + pubKeyN[0] + ", " + pubKeyN[1] + ", ...");
		System.out.println("priKeyN = [" + priKeyN.length + "] " + priKeyN[0] + ", " + priKeyN[1] + ", ...");
		System.out.println("check N in pubKey and priKey: " + Arrays.equals(pubKeyN, priKeyN));

		// 测试RSA加解密
		byte[] data0 = new byte[32];
		new SecureRandom().nextBytes(data0);
		byte[] data1 = Cert.encryptRsa(pubKey2, data0);
		byte[] data2 = Cert.decryptRsa(priKey2, data1);
		System.out.println("original   = [" + data0.length + "] " + data0[0] + ", " + data0[1] + ", ...");
		System.out.println("encryptRsa = [" + data1.length + "] " + data1[0] + ", " + data1[1] + ", ...");
		System.out.println("decryptRsa = [" + data2.length + "] " + data2[0] + ", " + data2[1] + ", ...");
		System.out.println("check decrypt data: " + Arrays.equals(data0, data2));
	}
}
/*
<bean name="BKeyExchangeArg">
	<variable id="1" name="version"         type="int"/>
	<variable id="2" name="clientPubKey"    type="binary"/>
	<variable id="3" name="serverPubKeyMd5" type="binary"/>
	<variable id="4" name="encIvKey"        type="binary"/>
</bean>
<bean name="BKeyExchangeRes">
	<enum value="1" name="ErrorUnknownClientPubKey"/>
	<enum value="2" name="ErrorUnknownServerPubKey"/>
	<enum value="3" name="ErrorDecryptFailed"/>

	<variable id="1" name="encIvKey" type="binary"/>
</bean>
<rpc name="KeyExchange" argument="BKeyExchangeArg" result="BKeyExchangeRes" handle="server"/>
*/
