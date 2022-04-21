package Zeze;

public abstract class AppBase {
	public abstract Application getZeze();

	public <T extends Zeze.IModule> T ReplaceModuleInstance(T in) {
		return in;
	}
}
