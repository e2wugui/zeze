package Zeze;

public abstract class AppBase {
	public abstract Application getZeze();

	public Zeze.IModule[] replaceModuleInstances(Zeze.IModule[] modules) {
		return modules;
	}
}
