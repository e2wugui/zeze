package Zeze.Game;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public final class ProtocolOnlineSpec extends EncodedPrototolOnlineSpec implements OnlineSpec {
	private final @NotNull Protocol<?> p; // 记录来判断类型和记录发送日志。实际使用构造的时候已经传递给super。

	ProtocolOnlineSpec(@NotNull Protocol<?> p) {
		super(p.getTypeId(), new Binary(p.encode()));
		this.p = p;
	}

	@Override
	public int send(@NonNull Online online) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", logName(online), p);
		return super.send(online);
	}
}
