package Temp;

public class ModuleA implements IModuleA {
	private final int version;

	public ModuleA(int version) {
		this.version = version;
	}

	@Override
	protected void finalize() {
		System.out.println("~ModuleA " + version);
	}

	public void callB(ClassLoaderApp.ModuleClassLoader loader) throws Exception {
		var b = (ModuleB)loader.loadClass("Temp.ModuleB").getConstructor(int.class).newInstance(1);
		b.println("hello world");

		/*
		ClassLoaderApp.instance.b.println("a call b");
		System.out.println("staticVar=" + ModuleB.staticVar);
		ModuleB.staticMethod();
		*/
	}
}
