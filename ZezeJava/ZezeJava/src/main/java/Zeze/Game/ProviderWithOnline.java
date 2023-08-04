package Zeze.Game;

import java.util.HashMap;
import java.util.function.Consumer;
import Zeze.AppBase;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProviderWithOnline extends ProviderImplement {
	protected Online online; // 默认的Online. 需要外面调用create方法创建并初始化。App.Start.
	protected final HashMap<String, Online> onlineSetMap = new HashMap<>(); // 所有创建过的Online,默认Online的key是空字符串. 需要外面调用默认Online.createOnlineSet创建

	public Online getOnline() {
		return online;
	}

	public @Nullable Online getOnline(@Nullable String name) {
		return name != null && !name.isEmpty() ? onlineSetMap.get(name) : online;
	}

	public void foreachOnline(@NotNull Consumer<Online> consumer) {
		onlineSetMap.values().forEach(consumer);
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG)
			logger.info("LinkBroken[{}]: {}", p.getSender().getSessionId(), AsyncSocket.toStr(p.Argument));
		// 目前仅需设置online状态。
		var online = this.online;
		if (!p.Argument.getUserState().getContext().isEmpty() && online != null) {
			var roleId = Long.parseLong(p.Argument.getUserState().getContext());
			var onlineSet = online.getOnline(p.Argument.getUserState().getOnlineSetName());
			if (null != onlineSet)
				onlineSet.linkBroken(p.Argument.getAccount(), roleId,
						ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}

	// 创建默认的Online和指定name的若干Online,重复创建会抛异常
	public synchronized void create(@NotNull AppBase app, @NotNull String... names) throws Exception {
		if (online != null)
			throw new IllegalStateException("duplicate default");
		online = Online.create(app);
		online.Initialize(app);
		onlineSetMap.put("", online);
		for (var name : names) {
			if (onlineSetMap.containsKey(name))
				throw new IllegalStateException("duplicate name='" + name + '\'');
			onlineSetMap.put(name, online.createOnlineSet(app, name));
		}
	}

	public synchronized void start() {
		online.start();
	}

	public synchronized void stop() {
		if (online != null) {
			online.stop();
			onlineSetMap.clear();
			online = null;
		}
	}
}
