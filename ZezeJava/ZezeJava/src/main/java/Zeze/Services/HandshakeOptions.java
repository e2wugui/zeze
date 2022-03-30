package Zeze.Services;

import java.util.HashSet;
import Zeze.Services.Handshake.Helper;
import Zeze.Util.IntHashSet;

/**
 * 使用dh算法交换密匙把连接加密。
 * 如果dh交换失败，现在依赖加密压缩实现以及后面的协议解析的错误检查来发现。
 * 有没有好的安全的dh交换失败的检测手段。
 * 服务器客户端定义在一起
 */
public class HandshakeOptions {
	private IntHashSet DhGroups = new IntHashSet(); // for HandshakeServer
	private byte[] SecureIp;
	private boolean S2cNeedCompress = true;
	private boolean C2sNeedCompress = true;
	private byte DhGroup = 1; // for HandshakeClient

	public HandshakeOptions() {
		AddDhGroup(1);
		AddDhGroup(2);
		AddDhGroup(5);
	}

	public final IntHashSet getDhGroups() {
		return DhGroups;
	}

	public final void setDhGroups(IntHashSet value) {
		if (value != null)
			DhGroups = value;
		else
			DhGroups.clear();
	}

	@Deprecated
	public final void setDhGroups(HashSet<Integer> value) {
		DhGroups.clear();
		if (value != null) {
			for (Integer v : value)
				DhGroups.add(v);
		}
	}

	public final byte[] getSecureIp() {
		return SecureIp;
	}

	public final void setSecureIp(byte[] value) {
		SecureIp = value;
	}

	public final boolean getS2cNeedCompress() {
		return S2cNeedCompress;
	}

	public final void setS2cNeedCompress(boolean value) {
		S2cNeedCompress = value;
	}

	public final boolean getC2sNeedCompress() {
		return C2sNeedCompress;
	}

	public final void setC2sNeedCompress(boolean value) {
		C2sNeedCompress = value;
	}

	public final byte getDhGroup() {
		return DhGroup;
	}

	public final void setDhGroup(byte value) {
		DhGroup = value;
	}

	public final void AddDhGroup(int group) {
		if (Helper.isDHGroupSupported(group))
			DhGroups.add(group);
	}
}
