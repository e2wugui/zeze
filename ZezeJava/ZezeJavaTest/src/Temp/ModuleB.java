package Temp;

public class ModuleB {
	public final int version;
	public static final int staticVar = 1;

	public static void staticMethod() {
		System.out.println("ModuleB staticMethod()");
	}

	public ModuleB(int version) {
		this.version = version;
	}

	@Override
	protected void finalize() {
		System.out.println("~ModuleB " + version);
	}

	public void println(String value) {
		System.out.println("ModuleB " + value + " version=" + version);
	}
}
