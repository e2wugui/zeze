package Zeze.Services.ZokerImpl;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import Zeze.Services.ZokerAgent;

public class ZokerAgentService extends Service {
	private final ZokerAgent agent;

	public ZokerAgentService(ZokerAgent agent, Config config) {
		super("Zeze.ZokerAgentService", config);
		this.agent = agent;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		var zokerName = (String)so.getUserState();
		// GE-C03（FND21）：条件移除——只摘本连接自己注册的条目。旧连接死亡被新 Register
		// 接管（ZokerAgent.ProcessRegisterRequest 的 replace）后，本连接迟到的 close 回调
		// 不得把继承者的注册一并摘除（原 remove(key) 无条件摘除会清掉接管者的条目，
		// 健康新连接立即失联）。自己仍是现主人时语义与原 remove(key) 等价。
		if (zokerName != null)
			agent.zokers().remove(zokerName, so);
		super.OnSocketClose(so, e);
	}
}
