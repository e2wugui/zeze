package Zeze.Services.ZokerImpl;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import Zeze.Services.ZokerAgent;

/**
 * ZokerAgent 的网络服务：接受 Zoker 连接并维护注册表；连接断开时按本连接注册过的
 * 全部名字条件移除 zokers 条目。
 */
public class ZokerAgentService extends Service {
	private final ZokerAgent agent;

	public ZokerAgentService(ZokerAgent agent, Config config) {
		super("Zeze.ZokerAgentService", config);
		this.agent = agent;
	}

	@Override
	public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
		// 预装名字集合（userState 载荷，ZokerAgent.RegisteredNames）：accept 先于本连接任何
		// 协议派发，Register 到达时集合必已就位——Register 侧惰性补装在同连接并发派发下会
		// 互相覆盖丢名（对齐 LinkdProviderService.OnSocketAccept 的预装形态）。
		so.setUserState(new ZokerAgent.RegisteredNames());
		super.OnSocketAccept(so);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		// 条件移除——只摘本连接自己注册的条目。旧连接死亡被新 Register
		// 接管（ZokerAgent.ProcessRegisterRequest 的 replace）后，本连接迟到的 close 回调
		// 不得把继承者的注册一并摘除（无条件 remove(key) 会清掉接管者的条目，
		// 健康新连接立即失联）。自己仍是现主人时摘自己的条目，语义不变。
		// 遍历本连接注册过的全部名字逐一条件摘除（换名注册的历史名一并收殓，零滞留），
		// 摘毕清空集合。
		if (so.getUserState() instanceof ZokerAgent.RegisteredNames registered) {
			registered.forEach(name -> agent.zokers().remove(name, so));
			registered.clear();
		}
		super.OnSocketClose(so, e);
	}
}
