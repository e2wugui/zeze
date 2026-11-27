package Zeze.Services.Handshake;

import java.util.Objects;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import java.util.function.Predicate;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Decrypt2;
import Zeze.Net.Digest;
import Zeze.Net.Encrypt2;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.BitConverter;
import Zeze.Util.Cert;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/**
 * RSA/AES 双向对称密钥交换 RPC：客户端明文发起并以服务端公钥加密客户端密钥（可选附带客户端公钥），
 * 服务端解密后生成服务端密钥并以异或或客户端公钥加密返回；双方随后切入双向 AES-128-CFB 加密通信。
 */
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
		public byte @Nullable [] clientPubKey; // 本地客户端公钥. RSA公钥中的N(modulus)以大端序列化成byte数组(最高位的字节不能为0). 作为回程密钥(服务器key)的加密目标；仅当服务器经addHandler重载注册了可信公钥校验时才构成身份认证，否则服务器不校验它(任何自造公钥都能建立会话)。可以为空(不使用公钥模式,回程密钥与客户端key异或后返回)
		public byte @Nullable [] serverPubKeyMd5; // 对方服务器公钥的MD5. 如上方法序列化后再做MD5, 用于对方选取所用的私钥
		public byte @Nullable [] encIvKey; // 随机生成对称加密的iv和key各16字节拼接成32字节,以"RSA/ECB/PKCS1Padding"模式加密

		@Override
		public int preAllocSize() {
			return 534; // 1 + 2+256 + 1+16 + 2+256
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBytes(clientPubKey != null ? clientPubKey : ByteBuffer.Empty);
			bb.WriteBytes(serverPubKeyMd5 != null ? serverPubKeyMd5 : ByteBuffer.Empty);
			bb.WriteBytes(encIvKey != null ? encIvKey : ByteBuffer.Empty);
		}

		@Override
		public void decode(IByteBuffer bb) {
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
		public static final int ErrorUnknownClientPubKey = 1; // 未知或非法的客户端公钥(仅当服务器经addHandler重载启用客户端公钥认证时返回;clientPubKey为空时表示该服务器要求客户端公钥认证)
		public static final int ErrorUnknownServerPubKey = 2; // 未知或非法的服务器公钥
		public static final int ErrorDecryptFailed = 3; // 解密encIvKey失败

		public int version; // 版本. 同Arg的version
		public byte @Nullable [] encIvKey; // 如果clientPubKey为空,表示双方随机生成的对称加密的iv和key的异或结果; 否则类似Arg中的encIvKey,给客户端加密发送服务器随机生成的iv和key

		@Override
		public int preAllocSize() {
			return 259; // 1 + 2+256
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBytes(encIvKey != null ? encIvKey : ByteBuffer.Empty);
		}

		@Override
		public void decode(IByteBuffer bb) {
			version = bb.ReadInt();
			if (version != 0)
				throw new UnsupportedOperationException("version = " + version);
			encIvKey = bb.ReadBytes();
		}
	}

	private byte @Nullable [] clientIvKey; // 仅客户端用,在服务器处理时客户端暂存自己生成的iv和key

	public KeyExchange() {
		Argument = new Arg();
		Result = new Res();
	}

	public KeyExchange(byte [] serverPubKey) {
		this(serverPubKey, null);
	}

	public KeyExchange(byte [] serverPubKey, byte @Nullable [] clientPubKey) {
		this();
		Argument.clientPubKey = clientPubKey;
		Argument.serverPubKeyMd5 = getPubKeyMd5(serverPubKey);
		Argument.encIvKey = encryptRsa(serverPubKey, clientIvKey = genIvKey());
	}

	public static byte [] getPubKeyMd5(byte [] pubKey) {
		return Digest.md5(stripLeadingZeros(pubKey));
	}

	// 跳过公钥前导字节0（BigInteger.toByteArray对正数可能带符号位前导0），同模数不同编码归一
	private static byte [] stripLeadingZeros(byte [] pubKey) {
		int i = 0;
		for (; i < pubKey.length; i++) {
			if (pubKey[i] != 0)
				break;
		}
		return i == 0 ? pubKey : Arrays.copyOfRange(pubKey, i, pubKey.length);
	}

	public static byte [] encryptRsa(byte [] pubKeyN, byte [] data) {
		try {
			var rsaPubKey = Cert.loadRsaPublicKey(new BigInteger(1, pubKeyN), pubKeyE);
			return Cert.encryptRsa(rsaPubKey, data);
		} catch (GeneralSecurityException e) {
			throw Task.forceThrow(e);
		}
	}

	// SecureRandom线程安全，静态复用：每次new有种子初始化开销（部分实现还要读熵源）
	private static final SecureRandom ivKeyRandom = new SecureRandom();

	public static byte [] genIvKey() {
		byte[] ivKey = new byte[32];
		ivKeyRandom.nextBytes(ivKey);
		return ivKey;
	}

	public boolean send(AsyncSocket so) {
		return send(so, null);
	}

	public boolean send(AsyncSocket so, @Nullable PrivateKey clientPriKey) {
		if ((Argument.clientPubKey == null) != (clientPriKey == null)) // 客户端公私钥必须同时提供
			throw new IllegalArgumentException();
		return Send(so, r -> {
			try {
				if (r.getResultCode() != 0)
					return r.getResultCode(); // 错误应答的Result.encIvKey为null，不能进入密钥推导
				byte[] serverIvKey;
				int serverIvKeyLen;
				if (clientPriKey != null) {
					serverIvKey = Cert.decryptRsa(clientPriKey, Objects.requireNonNull(Result.encIvKey));
					if (serverIvKey.length != 32)
						throw new IllegalStateException("invalid serverIvKey length = " + serverIvKey.length);
				} else {
					serverIvKey = Result.encIvKey;
					//noinspection DataFlowIssue
					serverIvKeyLen = serverIvKey.length;
					if (serverIvKeyLen != 32)
						throw new IllegalStateException("invalid serverIvKey length = " + serverIvKeyLen);
					for (int i = 0; i < 32; i++)
						//noinspection DataFlowIssue
						serverIvKey[i] ^= clientIvKey[i];
				}
				byte[] serverIv = Arrays.copyOfRange(serverIvKey, 0, 16);
				byte[] serverKey = Arrays.copyOfRange(serverIvKey, 16, 32);
				Objects.requireNonNull(((TcpSocket)r.getSender())).setOutputSecurityCodec((__, outBuf) -> new Encrypt2(outBuf, serverKey, serverIv));

				byte[] clientIv = Arrays.copyOfRange(Objects.requireNonNull(clientIvKey), 0, 16);
				byte[] clientKey = Arrays.copyOfRange(clientIvKey, 16, 32);
				((TcpSocket)r.getSender()).setInputSecurityCodec((__, inBuf) -> new Decrypt2(inBuf, clientKey, clientIv));
				return 0;
			} catch (Throwable ex) {
				// 密钥交换异常可能已切换部分编解码器，直接断连
				//noinspection DataFlowIssue
				r.getSender().close(ex);
				return Res.ErrorDecryptFailed;
			}
		});
	}

	public long processKeyExchangeRequest(PrivateKey priKey, byte @Nullable [] pubKeyMd5,
										  @Nullable Predicate<byte []> clientPubKeyAcceptor) {
		try {
			// 先校验客户端公钥再变更连接状态，失败时优雅关闭以确保错误码先刷出
			if (clientPubKeyAcceptor != null) {
				var clientPubKey = stripLeadingZeros(Objects.requireNonNull(Argument.clientPubKey));
				if (clientPubKey.length == 0 || !clientPubKeyAcceptor.test(clientPubKey)) {
					trySendResultCode(Res.ErrorUnknownClientPubKey);
					//noinspection DataFlowIssue
					getSender().closeGracefully();
					return 0;
				}
			}
			if (!Arrays.equals(Argument.serverPubKeyMd5, pubKeyMd5)) {
				trySendResultCode(Res.ErrorUnknownServerPubKey);
				return 0;
			}
			byte[] clientIvKey;
			try {
				clientIvKey = Cert.decryptRsa(priKey, Objects.requireNonNull(Argument.encIvKey));
			} catch (GeneralSecurityException e) {
				throw Task.forceThrow(e);
			}
			if (clientIvKey.length != 32)
				throw new IllegalStateException("invalid clientIvKey length = " + clientIvKey.length);

			byte[] serverIvKey = genIvKey();
			byte[] serverIv = Arrays.copyOfRange(serverIvKey, 0, 16);
			byte[] serverKey = Arrays.copyOfRange(serverIvKey, 16, 32);
			Objects.requireNonNull(((TcpSocket)getSender())).setInputSecurityCodec((__, inBuf) -> new Decrypt2(inBuf, serverKey, serverIv));

			//noinspection DataFlowIssue
			if (Argument.clientPubKey.length > 0) {
				Result.encIvKey = encryptRsa(Argument.clientPubKey, serverIvKey);
			} else {
				for (int i = 0; i < 32; i++)
					serverIvKey[i] ^= clientIvKey[i];
				Result.encIvKey = serverIvKey;
			}
			trySendResultCode(0);

			byte[] clientIv = Arrays.copyOfRange(clientIvKey, 0, 16);
			byte[] clientKey = Arrays.copyOfRange(clientIvKey, 16, 32);
			((TcpSocket)getSender()).setOutputSecurityCodec((__, outBuf) -> new Encrypt2(outBuf, clientKey, clientIv));
			return 0;
		} catch (Throwable ex) { // 握手错误必须断连，防止半开或滥用私钥解密
			//noinspection DataFlowIssue
			getSender().close(ex);
			return 0;
		}
	}

	/**
	 * 注册 KeyExchange 处理器（不校验客户端公钥）并为目标 Service 装配未握手明文门禁；须在建立连接前调用。
	 */
	public static void addHandler(Service service, PrivateKey serverPriKey) {
		addHandler(service, serverPriKey, null);
	}

	/**
	 * 注册 KeyExchange 处理器、装配未握手明文门禁，并启用客户端公钥校验（传入已去前导零的模数 N）。
	 * 未提供公钥或校验失败时回复 {@link Res#ErrorUnknownClientPubKey} 并断连；须在建立连接前调用。
	 */
	public static void addHandler(Service service, PrivateKey serverPriKey,
								  @Nullable Predicate<byte []> clientPubKeyAcceptor) {
		service.armDecodeAdmission(KeyExchange.TypeId);
		byte[] pubKeyMd5 = getPubKeyMd5(((RSAKey)serverPriKey).getModulus().toByteArray());
		if (!service.getFactorys().containsKey(KeyExchange.TypeId)) {
			service.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
					r -> r.processKeyExchangeRequest(serverPriKey, pubKeyMd5, clientPubKeyAcceptor),
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

	public static void main(String[] args) throws GeneralSecurityException, IOException {
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

		if (args.length == 2 && args[0].equals("-gen")) {
			Files.write(Path.of(args[1]), priKeyData, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			int i = 0;
			while (pubKeyN[i] == 0)
				i++;
			System.out.println("RsaPubKey=\"" + BitConverter.toHexString(pubKeyN, i, pubKeyN.length - i) + '"');
			System.out.println("{");
			for (int j = 0; i < pubKeyN.length; i++) {
				System.out.format(" 0x%02X,", pubKeyN[i] & 0xff);
				if (++j == 16) {
					j = 0;
					System.out.println();
				}
			}
			System.out.println("};");
		}
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
