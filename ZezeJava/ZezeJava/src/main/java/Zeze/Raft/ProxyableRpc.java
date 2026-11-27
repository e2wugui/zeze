package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.Serializable;
import org.jspecify.annotations.Nullable;

/**
 * 可走代理的 Rpc：持有 ProxyRequest，所有应答入口在代理路径下改经 proxyRequest 应答。
 */
public abstract class ProxyableRpc<A extends Serializable, R extends Serializable> extends Rpc<A, R> {
	private @Nullable ProxyRequest proxyRequest;

	public void setProxyRequest(@Nullable ProxyRequest proxyRequest) {
		this.proxyRequest = proxyRequest;
	}

	@Override
	public void SendResult(@Nullable Binary result) {
		if (proxyRequest == null) {
			super.SendResult(result);
			return;
		}

		if (!tryMarkSendResultDone()) {
			logger.warn("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		resultEncoded = result;
		setRequest(false);

		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
	}

	// 代理模式下内层无独立 sender，需先取得 CAS 发送权，再经 proxyRequest 应答。
	@Override
	protected boolean sendResultCode(long code, @Nullable Binary result) {
		if (proxyRequest == null) {
			return super.sendResultCode(code, result);
		}

		if (!tryMarkSendResultDone())
			return false;
		setResultCode(code);
		resultEncoded = result;
		setRequest(false);

		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
		return true;
	}
}
