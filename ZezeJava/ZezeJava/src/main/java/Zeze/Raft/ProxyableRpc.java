package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.Nullable;

/**
 * 可走代理的 Rpc：持有 ProxyRequest，所有应答入口在代理路径下改经 proxyRequest 应答。
 */
public abstract class ProxyableRpc<A extends Serializable, R extends Serializable> extends Rpc<A, R> {
	private ProxyRequest proxyRequest;

	public void setProxyRequest(ProxyRequest proxyRequest) {
		this.proxyRequest = proxyRequest;
	}

	@Override
	public void SendResult(@Nullable Binary result) {
		if (proxyRequest == null) {
			// 原始raft连接方式。
			super.SendResult(result);
			return;
		}

		if (!tryMarkSendResultDone()) {
			logger.warn("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
	}

	// Protocol的两个SendResultCode和Rpc.trySendResultCode共用这个多态入口。
	// 代理内层通常没有sender，不能直接发送；先取得CAS发送权，再改字段并走代理。
	@Override
	protected boolean sendResultCode(long code, @Nullable Binary result) {
		if (proxyRequest == null) {
			// 原始raft连接方式。
			return super.sendResultCode(code, result);
		}

		if (!tryMarkSendResultDone())
			return false;
		setResultCode(code);
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。Rpc.encode 带 BitResultCode，解码侧码可达。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
		return true;
	}
}
