package Zeze;

public abstract class AppBase {
	public abstract Application getZeze();

	public <T extends Zeze.IModule> T replaceModuleInstance(T in) {
		return in;
	}
}
