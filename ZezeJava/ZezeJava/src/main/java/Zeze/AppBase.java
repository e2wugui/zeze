package Zeze;

public abstract class AppBase {
	public abstract Application getZeze();

	public Zeze.IModule ReplaceModuleInstance(Zeze.IModule in) {
		return in;
	}
}
