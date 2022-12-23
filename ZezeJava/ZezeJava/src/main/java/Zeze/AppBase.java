package Zeze;

import Zeze.Arch.Gen.GenModule;

public abstract class AppBase {
	public abstract Application getZeze();

	public IModule[] createRedirectModules(Class<?>[] moduleClasses) {
		return GenModule.instance.createRedirectModules(this, moduleClasses);
	}
}
