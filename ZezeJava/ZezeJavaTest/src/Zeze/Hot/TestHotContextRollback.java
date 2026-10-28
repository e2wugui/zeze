package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import Zeze.IModule;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHotContextRollback {
	@TempDir
	Path directory;

	static final class Service implements IModule, HotService {
		boolean failUpgrade;
		@Override public String getFullName() { return "test.context"; }
		@Override public String getName() { return "context"; }
		@Override public int getId() { return 0; }
		@Override public void start() { }
		@Override public void stop() { }
		@Override public void upgrade(HotService old) {
			if (failUpgrade)
				throw new IllegalStateException("upgrade failed");
		}
	}

	private HotModule module(String name, Service service) throws Exception {
		var jar = directory.resolve(name + ".jar");
		try (var ignored = new JarOutputStream(Files.newOutputStream(jar))) { }
		var module = new HotModule(jar.toFile(), "test.context.Schemas");
		module.setService(service);
		return module;
	}

	@Test
	void failedUpgradeRestoresExternalContext() throws Exception {
		var oldService = new Service();
		var newService = new Service();
		newService.failUpgrade = true;
		try (var old = module("old", oldService); var next = module("next", newService)) {
			var context = old.getContext(Service.class);
			assertThrows(IllegalStateException.class, () -> next.upgrade(old));
			assertSame(oldService, context.getService());
		}
	}

	@Test
	void successfulUpgradeRebindsExternalContext() throws Exception {
		var oldService = new Service();
		var newService = new Service();
		try (var old = module("old", oldService); var next = module("next", newService)) {
			var context = old.getContext(Service.class);
			next.upgrade(old);
			assertSame(newService, context.getService());
		}
	}

	@Test
	void laterInstallationFailureRestoresEarlierSuccessfulRebindings() throws Exception {
		var oldService = new Service();
		var newService = new Service();
		try (var old = module("old", oldService); var next = module("next", newService)) {
			var context = old.getContext(Service.class);
			var transaction = new HotTransaction("context rollback");
			next.upgrade(old, transaction);
			assertSame(newService, context.getService());
			transaction.rollback();
			transaction.rollback();
			assertSame(oldService, context.getService());
		}
	}
}
