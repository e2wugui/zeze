package Zeze.MQ;

import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class MQConfig implements Config.ICustomize {
	private int rpcTimeout = 20_000;

	public int getRpcTimeout() {
		return rpcTimeout;
	}

	public void setRpcTimeout(int value) {
		rpcTimeout = value;
	}

	@Override
	public @NotNull String getName() {
		return "MQConfig";
	}

	@Override
	public void parse(Element self) {
		var attr = self.getAttribute("RpcTimeout");
		if (!attr.isBlank())
			rpcTimeout = Integer.parseInt(attr);
	}
}
