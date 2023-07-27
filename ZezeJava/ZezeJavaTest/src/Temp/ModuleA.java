package Temp;

public class ModuleA {
	private final int version;

	public ModuleA(int version) {
		this.version = version;
	}

	@Override
	protected void finalize() {
		System.out.println("~ModuleA " + version);
	}

	public void callB() {
		ClassLoaderApp.instance.b.println("a call b");
		System.out.println("staticVar=" + ModuleB.staticVar);
		ModuleB.staticMethod();
	}
}
