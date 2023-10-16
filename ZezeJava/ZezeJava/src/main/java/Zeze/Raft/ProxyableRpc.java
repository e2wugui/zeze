package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.Nullable;

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

		// proxy 方式，基本逻辑拷贝自 Rpc.SendResult(Binary result)。
		if (sendResultDone) {
			logger.error("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		sendResultDone = true;
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
	}
}
