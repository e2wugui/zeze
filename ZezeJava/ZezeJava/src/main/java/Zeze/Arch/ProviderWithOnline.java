package Zeze.Arch;

import java.util.Objects;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/**
 * 带在线表的 Provider 实现模板：组合 Arch.Online，处理 LinkBroken 下线与负载上报。
 */
public class ProviderWithOnline extends ProviderImplement {
	protected @Nullable Online online; // 需要外面初始化。App.Start.
	private @Nullable ProviderLoadWithOnline load;

	public @Nullable Online getOnline() {
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
			// 失败码外传整体回滚（对齐Online.linkBroken的内部外传契约与Game版入口）。
			return Objects.requireNonNull(online).linkBroken(p.Argument.getAccount(), p.Argument.getUserState().getContext(),
					ProviderService.getLinkName(Objects.requireNonNull(p.getSender())), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}

	public void create(AppBase app) throws Exception {
		online = Online.create(app);
		online.Initialize(app);
		load = new ProviderLoadWithOnline(online);
		//noinspection DataFlowIssue
		var config = app.getZeze().getConfig();
		load.getOverload().register(Objects.requireNonNull(Task.getThreadPool()), config);
	}

	public void start() {
		//noinspection DataFlowIssue
		load.start();
		//noinspection DataFlowIssue
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
