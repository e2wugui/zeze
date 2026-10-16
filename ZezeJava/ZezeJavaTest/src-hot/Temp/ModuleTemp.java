package Temp;

import java.util.Objects;
import Zeze.Hot.HotService;
import org.jspecify.annotations.NullMarked;
import Zeze.IModule;

@NullMarked
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
		var service = Objects.requireNonNull(module).getService();
		if (service == this)
			System.out.println("Just Me!");
		System.out.println(module.getClass().getClassLoader());
		System.out.println(Objects.requireNonNull(service).getClass().getClassLoader());
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
