package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.math.*;

/** 
 使用dh算法交换密匙把连接加密。
 如果dh交换失败，现在依赖加密压缩实现以及后面的协议解析的错误检查来发现。
 有没有好的安全的dh交换失败的检测手段。
*/


/** 
 服务器客户端定义在一起
*/
public class HandshakeOptions {
	// for HandshakeServer
	private HashSet<Integer> DhGroups = new HashSet<Integer> ();
	public final HashSet<Integer> getDhGroups() {
		return DhGroups;
	}
	public final void setDhGroups(HashSet<Integer> value) {
		DhGroups = value;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] SecureIp = null;
	private byte[] SecureIp = null;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] getSecureIp()
	public final byte[] getSecureIp() {
		return SecureIp;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void setSecureIp(byte[] value)
	public final void setSecureIp(byte[] value) {
		SecureIp = value;
	}
	private boolean S2cNeedCompress = true;
	public final boolean getS2cNeedCompress() {
		return S2cNeedCompress;
	}
	public final void setS2cNeedCompress(boolean value) {
		S2cNeedCompress = value;
	}
	private boolean C2sNeedCompress = true;
	public final boolean getC2sNeedCompress() {
		return C2sNeedCompress;
	}
	public final void setC2sNeedCompress(boolean value) {
		C2sNeedCompress = value;
	}

	// for HandshakeClient
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte DhGroup = 1;
	private byte DhGroup = 1;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte getDhGroup()
	public final byte getDhGroup() {
		return DhGroup;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void setDhGroup(byte value)
	public final void setDhGroup(byte value) {
		DhGroup = value;
	}

	public HandshakeOptions() {
		AddDhGroup(1);
		AddDhGroup(2);
		AddDhGroup(5);
	}

	public final void AddDhGroup(int group) {
		if (Handshake.Helper.isDHGroupSupported(group)) {
			getDhGroups().add(group);
		}
	}
}