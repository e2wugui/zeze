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
			logger.warn("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		sendResultDone = true;
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
	}

	// 【FND2-R2-4】处理器异常的onError兜底走Rpc.trySendResultCode，代理路径下
	// getSender()==null，直发失败会静默丢弃错误码（客户端等满proxy超时并触发
	// 重发循环）；与SendResult(Binary)同构，proxy方式置码后经proxyRequest应答。
	@Override
	public boolean trySendResultCode(long code) {
		if (proxyRequest == null) {
			// 原始raft连接方式。
			return super.trySendResultCode(code);
		}

		// proxy 方式，基本逻辑拷贝自 Rpc.trySendResultCode(long)。
		if (sendResultDone) {
			logger.warn("Rpc.trySendResultCode Already Done: {} {}", getSender(), this, new Exception());
			return false;
		}
		sendResultDone = true;
		setResultCode(code);
		resultEncoded = null;
		setRequest(false);

		// 填写proxyRequest.Result并发送。Rpc.encode 带 BitResultCode，解码侧码可达。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
		return true;
	}
}
