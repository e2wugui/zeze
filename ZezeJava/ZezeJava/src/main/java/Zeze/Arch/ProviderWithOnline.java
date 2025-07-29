package Zeze.Arch;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.jetbrains.annotations.Nullable;

public class ProviderWithOnline extends ProviderImplement {
	protected Online online; // 需要外面初始化。App.Start.
	private ProviderLoadWithOnline load;

	public Online getOnline() {
		return online;
	}

	@Override
	public @Nullable ProviderLoadWithOnline getLoad() {
		return load;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		// 目前仅需设置online状态。
		if (!p.Argument.getUserState().getContext().isEmpty()) {
			online.linkBroken(p.Argument.getAccount(), p.Argument.getUserState().getContext(),
					ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}

	public void create(AppBase app) throws Exception {
		online = Online.create(app);
		online.Initialize(app);
		load = new ProviderLoadWithOnline(online);
		var config = app.getZeze().getConfig();
		load.getOverload().register(Task.getThreadPool(), config);
	}

	public void start() {
		load.start();
		online.start();
	}

	@Override
	public void stop() throws Exception {
		if (online != null)
			online.stop();
		if (load != null)
			load.stop();
	}
}
