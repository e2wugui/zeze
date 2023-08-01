package Temp;

import Zeze.Hot.HotManager;

public class ModuleA implements IModuleInterface {
	public ModuleA() {
	}

	@Override
	public void test(HotManager manager) {
		var module = manager.getModule("Temp");
		var service = module.<IModuleInterface>getService();
		if (service == this)
			System.out.println("Just Me!");
		System.out.println(module.getClass().getClassLoader());
		System.out.println(service.getClass().getClassLoader());
		service.helloWorld();
	}

	@Override
	public void helloWorld() {
		System.out.println("hello world.");
	}
}
