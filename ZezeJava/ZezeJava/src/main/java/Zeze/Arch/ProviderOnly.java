package Zeze.Arch;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Util.Task;
import org.jetbrains.annotations.Nullable;

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
	public void stop() {
		if (load != null)
			load.stop();
	}
}
