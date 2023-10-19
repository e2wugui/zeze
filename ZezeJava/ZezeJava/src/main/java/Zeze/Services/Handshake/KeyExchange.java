package Zeze.Services.Handshake;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

// TCP连接成功后,主动连接方(客户端)先发密钥交换请求(KeyExchange)给被动连接方(服务器). 收到回复后,后续通信用双方各自随机生成的key做双向对称加密(己方生成的key用于对方加密发送,己方接收解密)
// 客户端 === KeyExchange(明文请求) ==> 服务器 (客户端用服务器公钥加密客户端生成的key给服务器,可选提供客户端公钥)
// 客户端 <== KeyExchange(明文回复) === 服务器 (服务器用服务器私钥解密得到客户端的key,再生成服务器key,二者做异或回复给客户端,或者验证客户端公钥并用客户端公钥加密服务器key回给客户端,两种方法都能让客户端得到服务器key)
// 客户端 === 其它协议(使用服务器生成的key加密) ==> 服务器 (密钥交换后,需要由客户端先发起协议,方便客户端确定何时开始解密)
// 客户端 <== 其它协议(使用客户端生成的key加密) === 服务器
public final class KeyExchange extends Rpc<KeyExchange.Arg, KeyExchange.Res> {
	public static final int ModuleId = 0;
	public static final int ProtocolId = Bean.hash32(KeyExchange.class.getName()); // 571437512 0x220F71C8
	public static final long TypeId = Protocol.makeTypeId(ModuleId, ProtocolId); // 571437512 [00 00 00 00 C8 71 0F 22]

	static {
		register(TypeId, KeyExchange.class);
	}

	public static final class Arg implements Serializable {
		public int version; // 版本. 目前只定义初始版本:0, 即固定使用RSA-2048(exponent固定为65537)作为非对称加密,AES-128(CFB模式)作为对称加密
		public Binary clientPubKey; // 本地客户端公钥. RSA公钥中的modulus以大端序列化成byte数组(最高位的字节不能为0). 用于对方验证自己的身份,可以为空(不验证,仅单向非对称加密)
		public Binary serverPubKeyMd5; // 对方服务器公钥的MD5. 如上方法序列化后再做MD5, 用于对方选取所用的私钥
		public Binary encIvKey; // 如果pubKeyMd5不为空,则表示随机生成对称加密的iv和key各16字节拼接成32字节,以大端序使用对方公钥加密后再以大端序列化的结果; 否则表示双方随机生成的对称加密的iv和key的异或结果

		@Override
		public int preAllocSize() {
			return 540;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBinary(clientPubKey != null ? clientPubKey : Binary.Empty);
			bb.WriteBinary(serverPubKeyMd5 != null ? serverPubKeyMd5 : Binary.Empty);
			bb.WriteBinary(encIvKey != null ? encIvKey : Binary.Empty);
		}

		@Override
		public void decode(@NotNull ByteBuffer bb) {
			int v = bb.ReadInt();
			if (v != 0)
				throw new UnsupportedOperationException("version = " + version);
			version = v;
			clientPubKey = bb.ReadBinary();
			serverPubKeyMd5 = bb.ReadBinary();
			encIvKey = bb.ReadBinary();
		}
	}

	public static final class Res implements Serializable {
		// resultCode=0表示成功,其它见下方枚举定义
		public static final int ErrorUnknownClientPubKey = 1; // 未知或非法的客户端公钥(如果clientPubKey为空,则表示必须验证客户端公钥)
		public static final int ErrorUnknownServerPubKey = 2; // 未知或非法的服务器公钥
		public static final int ErrorDecryptFailed = 3; // 解密encIvKey失败

		public int version; // 版本. 目前只定义初始版本:0, 即固定使用RSA-2048(exponent固定为65537)作为非对称加密,AES-128(CFB模式)作为对称加密
		public Binary encIvKey; // 如果clientPubKey为空,表示双方随机生成的对称加密的iv和key的异或结果; 否则类似请求中的encIvKey,给客户端发送服务器随机生成的iv和key

		@Override
		public int preAllocSize() {
			return 260;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(version);
			bb.WriteBinary(encIvKey != null ? encIvKey : Binary.Empty);
		}

		@Override
		public void decode(@NotNull ByteBuffer bb) {
			int v = bb.ReadInt();
			if (v != 0)
				throw new UnsupportedOperationException("version = " + version);
			version = v;
			encIvKey = bb.ReadBinary();
		}
	}

	public KeyExchange() {
		Argument = new Arg();
		Result = new Res();
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
