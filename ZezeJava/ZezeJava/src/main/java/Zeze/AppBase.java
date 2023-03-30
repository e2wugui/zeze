package Zeze;

import Zeze.Arch.Gen.GenModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AppBase {
	public abstract @NotNull Application getZeze();

	public @Nullable IModule[] createRedirectModules(Class<?> @NotNull [] moduleClasses) {
		return GenModule.instance.createRedirectModules(this, moduleClasses);
	}
}
