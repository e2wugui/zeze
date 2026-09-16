package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Rpc;

/**
 * GCM 登录协议。客户端契约：收到本协议成功应答之后才允许发送 Acquire/Release——
 * 应答前乐观预发的 Acquire 会被服务端 Login 的旧权限回收当作残留错误回收。
 * 框架随附 GlobalClient 已保证此顺序，自定义客户端必须遵守。
 */
public class Login extends Rpc<BLoginParam, BAchillesHeelConfig> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(Login.class.getName()); // -1420506365
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2874460931

	static {
		register(TypeId_, Login.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Login() {
		Argument = new BLoginParam();
		Result = new BAchillesHeelConfig();
	}
}
