package Zeze.Services;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端握手服务基类：重载OnSocketConnected推迟OnHandshakeDone到握手协议交换完成后。
 */
public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(@NotNull String name, @Nullable Config config) {
		super(name, config);
		addHandshakeClientFactoryHandle();
	}

	public HandshakeClient(@NotNull String name, @Nullable Application app) {
		super(name, app);
		addHandshakeClientFactoryHandle();
	}

	@Override
	public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
		// 重载这个方法，推迟OnHandshakeDone调用
		if (!addSocket(so)) // 撞号连接已被addSocket关闭，不得再走后续接受流程
			return;
	}
}
