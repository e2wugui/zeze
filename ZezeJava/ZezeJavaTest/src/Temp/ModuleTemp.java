package Temp;

import Zeze.Hot.HotService;
import Zeze.IModule;

public class ModuleTemp implements IModuleInterface, IModule {
	private final TempApp app;

	public ModuleTemp(TempApp app) {
		this.app = app;
	}

	@Override
	public void stop() {

	}

	@Override
	public void upgrade(HotService old) {

	}

	@Override
	public void start() {
		var manager = app.manager;
		var module = manager.getModuleContext("Temp", IModuleInterface.class);
		var service = module.getService();
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

	@Override
	public String getFullName() {
		return "Temp";
	}

	@Override
	public String getName() {
		return "Temp";
	}

	@Override
	public int getId() {
		return 0;
	}
}
