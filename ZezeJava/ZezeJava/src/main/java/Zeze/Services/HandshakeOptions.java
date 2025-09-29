package Zeze.Services;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import Zeze.Services.Handshake.Constant;
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
	private byte[] secureIp; // for eEncryptTypeAes
	private PublicKey rsaPubKey; // for eEncryptTypeRsaAes client
	private PrivateKey rsaPriKey; // for eEncryptTypeRsaAes server
	private int compressS2c = Constant.eCompressTypeDisable;
	private int compressC2s = Constant.eCompressTypeDisable;
	private int encryptType = Constant.eEncryptTypeDisable;

	private final ArrayList<Integer> supportedCompress = new ArrayList<>();
	private final ArrayList<Integer> supportedEncrypt = new ArrayList<>();

	private int keepCheckPeriod; // 检查所有socket是否有发送或接收超时的检查周期(秒). 0表示禁用(下面两个超时都不会生效)
	private int keepRecvTimeout; // 检查距上次接收的超时时间(秒). 0表示禁用, 超时会触发onKeepAliveTimeout, 通常会认为对方无响应而关闭连接
	private int keepSendTimeout; // 检查距上次发送的超时时间(秒). 0表示禁用, 可以只让连接的某一方启用, 超时会触发onSendKeepAlive, 通常是发KeepAlive的RPC, 需要对方回复

	public HandshakeOptions() {
		addDhGroup(1);
		addDhGroup(2);
		addDhGroup(5);

		addSupportedCompress(Constant.eCompressTypeZstd);
		addSupportedCompress(Constant.eCompressTypeMppc);

		addSupportedEncrypt(Constant.eEncryptTypeAes);
		addSupportedEncrypt(Constant.eEncryptTypeAesNoSecureIp);
		addSupportedEncrypt(Constant.eEncryptTypeRsaAes);
	}

	public int getKeepCheckPeriod() {
		return keepCheckPeriod;
	}

	public void setKeepCheckPeriod(int value) {
		keepCheckPeriod = value;
	}

	public int getKeepRecvTimeout() {
		return keepRecvTimeout;
	}

	public void setKeepRecvTimeout(int value) {
		keepRecvTimeout = value;
	}

	public int getKeepSendTimeout() {
		return keepSendTimeout;
	}

	public void setKeepSendTimeout(int value) {
		keepSendTimeout = value;
	}

	public final IntHashSet getDhGroups() {
		return dhGroups;
	}

	public ArrayList<Integer> getSupportedCompress() {
		return supportedCompress;
	}

	public ArrayList<Integer> getSupportedEncrypt() {
		return supportedEncrypt;
	}

	public boolean isSupportedCompress(int c) {
		return supportedCompress.contains(c);
	}

	public boolean isSupportedEncrypt(int e) {
		return supportedEncrypt.contains(e);
	}

	public void addSupportedEncrypt(int e) {
		supportedEncrypt.add(e);
	}

	public void addSupportedCompress(int c) {
		supportedCompress.add(c);
	}

	public final void setDhGroups(IntHashSet value) {
		if (value != null)
			dhGroups = value;
		else
			dhGroups.clear();
	}

	public final int getEncryptType() {
		return encryptType;
	}

	public final void setEncryptType(int value) {
		encryptType = value;
	}

	public final byte[] getSecureIp() {
		return secureIp;
	}

	public final void setSecureIp(byte[] value) {
		secureIp = value;
	}

	public final PublicKey getRsaPubKey() {
		return rsaPubKey;
	}

	public final void setRsaPubKey(PublicKey rsaPubKey) {
		this.rsaPubKey = rsaPubKey;
	}

	public final PrivateKey getRsaPriKey() {
		return rsaPriKey;
	}

	public final void setRsaPriKey(PrivateKey rsaPriKey) {
		this.rsaPriKey = rsaPriKey;
	}

	public final int getCompressS2c() {
		return compressS2c;
	}

	public final void setCompressS2c(int value) {
		compressS2c = value;
	}

	public final int getCompressC2s() {
		return compressC2s;
	}

	public final void setCompressC2s(int value) {
		compressC2s = value;
	}

	public final void addDhGroup(int group) {
		if (Helper.isDHGroupSupported(group))
			dhGroups.add(group);
	}
}
