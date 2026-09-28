package Zeze.Arch;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Util.Task;
import org.jetbrains.annotations.Nullable;

/**
 * 不带在线功能的纯 Provider 实现模板：仅做负载上报，忽略 LinkBroken。
 */
public class ProviderOnly extends ProviderImplement {
	private ProviderLoadOnly load;

	@Override
	public @Nullable ProviderLoadOnly getLoad() {
		return load;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) {
		return 0;
	}

	public void create(AppBase app) {
		load = new ProviderLoadOnly(app.getZeze());
		var config = app.getZeze().getConfig();
		load.getOverload().register(Task.getThreadPool(), config);
	}

	public void start() {
		load.start();
	}

	@Override
	public void stop() throws Exception {
		if (load != null)
			load.stop();
	}
}
