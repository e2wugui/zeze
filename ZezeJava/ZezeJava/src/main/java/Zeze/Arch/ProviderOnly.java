package Zeze.Arch;

import java.util.Objects;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/**
 * 不带在线功能的纯 Provider 实现模板：仅做负载上报，忽略 LinkBroken。
 */
public class ProviderOnly extends ProviderImplement {
	private @Nullable ProviderLoadOnly load;

	@Override
	public @Nullable ProviderLoadOnly getLoad() {
		return load;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) {
		return 0;
	}

	public void create(AppBase app) {
		load = new ProviderLoadOnly(Objects.requireNonNull(app.getZeze()));
		var config = Objects.requireNonNull(app.getZeze()).getConfig();
		load.getOverload().register(Objects.requireNonNull(Task.getThreadPool()), config);
	}

	public void start() {
		Objects.requireNonNull(load).start();
	}

	@Override
	public void stop() throws Exception {
		if (load != null)
			load.stop();
	}
}
