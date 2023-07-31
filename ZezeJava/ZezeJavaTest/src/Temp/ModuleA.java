package Temp;

public class ModuleA implements IModuleInterface {
	public ModuleA() {
	}

	@Override
	protected void finalize() {
		System.out.println("~ModuleA ");
	}

	@Override
	public void helloWorld() {
		System.out.println("hello world.");
	}
}
