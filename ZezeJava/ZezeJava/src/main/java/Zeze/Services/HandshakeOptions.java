package Zeze.Services;

import Zeze.Services.Handshake.Helper;
import Zeze.Util.IntHashSet;

/**
 * 使用dh算法交换密匙把连接加密。
 * 如果dh交换失败，现在依赖加密压缩实现以及后面的协议解析的错误检查来发现。
 * 有没有好的安全的dh交换失败的检测手段。
 * 服务器客户端定义在一起
 */
public class HandshakeOptions {
	private IntHashSet dhGroups = new IntHashSet(); // for HandshakeServer
	private byte[] secureIp;
	private boolean s2cNeedCompress = true;
	private boolean c2sNeedCompress = true;
	private byte dhGroup = 1; // for HandshakeClient
	private boolean enableEncrypt = false;

	public HandshakeOptions() {
		addDhGroup(1);
		addDhGroup(2);
		addDhGroup(5);
	}

	public final IntHashSet getDhGroups() {
		return dhGroups;
	}

	public final void setDhGroups(IntHashSet value) {
		if (value != null)
			dhGroups = value;
		else
			dhGroups.clear();
	}

	public final boolean getEnableEncrypt() {
		return enableEncrypt;
	}

	public final void setEnableEncrypt(boolean value) {
		enableEncrypt = value;
	}

	public final byte[] getSecureIp() {
		return secureIp;
	}

	public final void setSecureIp(byte[] value) {
		secureIp = value;
	}

	public final boolean getS2cNeedCompress() {
		return s2cNeedCompress;
	}

	public final void setS2cNeedCompress(boolean value) {
		s2cNeedCompress = value;
	}

	public final boolean getC2sNeedCompress() {
		return c2sNeedCompress;
	}

	public final void setC2sNeedCompress(boolean value) {
		c2sNeedCompress = value;
	}

	public final byte getDhGroup() {
		return dhGroup;
	}

	public final void setDhGroup(byte value) {
		dhGroup = value;
	}

	public final void addDhGroup(int group) {
		if (Helper.isDHGroupSupported(group))
			dhGroups.add(group);
	}
}
