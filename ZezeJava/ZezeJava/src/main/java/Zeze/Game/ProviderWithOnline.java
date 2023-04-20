package Zeze.Game;

import java.util.HashMap;
import Zeze.AppBase;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;

public class ProviderWithOnline extends ProviderImplement {
	protected Online online; // 需要外面初始化。App.Start.

	public Online getOnline() {
		return online;
	}

	private final HashMap<String, Online> onlineSetMap = new HashMap<>();

	@NotNull HashMap<String, Online> getOnlineSetMap() {
		return onlineSetMap;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		// 目前仅需设置online状态。
		if (!p.Argument.getUserState().getContext().isEmpty()) {
			var roleId = Long.parseLong(p.Argument.getUserState().getContext());
			online.linkBroken(p.Argument.getAccount(), roleId,
					ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid(),
					p.Argument.getUserState().getOnlineSetName());
		}
		return Procedure.Success;
	}

	public void create(@NotNull AppBase app) {
		online = Online.create(app);
		online.Initialize(app);
	}

	public void create(@NotNull AppBase app, @NotNull Class<? extends Bean> userDataClass) {
		create(app);
		Online.register(userDataClass);
	}

	public void start() {
		online.start();
	}

	public void stop() {
		if (online != null)
			online.stop();
	}
}
