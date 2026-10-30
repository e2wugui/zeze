package Zeze.Component;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated // HotManager构造会登记进程级BeanFactory的Application。
public class TestTimerColdCaller {
	@TempDir
	Path directory;

	public static class Handle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
		}
	}

	private static final class GameOnline extends Zeze.Game.Online {
		GameOnline(AppBase app) {
			super(app);
		}
	}

	private static final class ArchOnline extends Zeze.Arch.Online {
		ArchOnline(AppBase app) {
			super(app);
		}
	}

	private static final List<String> ROLE_CALLS = List.of(
			"r.scheduleOnline(1, spec, h, null)",
			"r.scheduleOnlineNamed(1, \"named\", spec, h, null)",
			"s.schedule(spec, h, null)",
			"s.scheduleNamed(\"named\", spec, h, null)",
			"s.schedule(spec, h)",
			"s.scheduleNamed(\"named\", spec, h)",
			"r.scheduleOnline(1, 1000, 1000, -1, -1, h, null)",
			"r.scheduleOnline(1, \"* * * * * ?\", -1, -1, h, null)",
			"r.scheduleOnlineNamed(1, \"named\", 1000, 1000, -1, -1, h, null)",
			"r.scheduleOnlineNamed(1, \"named\", \"* * * * * ?\", -1, -1, h, null)",
			"r.scheduleOnline(1, 1000, 1000, -1, -1, h, null, \"key\")",
			"r.scheduleOnline(1, \"* * * * * ?\", -1, -1, h, null, \"key\")",
			"r.scheduleOnlineNamed(1, \"named\", 1000, 1000, -1, -1, h, null, \"key\")",
			"r.scheduleOnlineNamed(1, \"named\", \"* * * * * ?\", -1, -1, h, null, \"key\")");

	private HotModule newModule(HotManager manager) throws Exception {
		var source = new StringBuilder("""
				package timerhot.Entry;
				import Zeze.Component.*;
				public class ModuleEntry {
				public static void call(TimerRole r, TimerAccount a, TimerScope s,
				                        Class<? extends TimerHandle> h, int entry, boolean account) {
				    var spec = TimerSpec.ofDelay(1000);
				    if (account) { switch(entry) {
				""");
		for (int i = 0; i < ROLE_CALLS.size(); i++) {
			var accountCall = ROLE_CALLS.get(i)
					.replace("r.scheduleOnlineNamed(1,", "a.scheduleOnlineNamed(\"account\", \"client\",")
					.replace("r.scheduleOnline(1,", "a.scheduleOnline(\"account\", \"client\",");
			source.append("case ").append(i).append(" -> ").append(accountCall).append(";\n");
		}
		source.append("default -> throw new AssertionError(entry); } } else { switch(entry) {\n");
		for (int i = 0; i < ROLE_CALLS.size(); i++)
			source.append("case ").append(i).append(" -> ").append(ROLE_CALLS.get(i)).append(";\n");
		source.append("default -> throw new AssertionError(entry); } } } }");
		var name = "timerhot.Entry.ModuleEntry";
		var bytes = new InMemoryJavaCompiler().compileAllToByteCode(Map.of(name, source.toString()));
		var file = directory.resolve("caller.jar");
		try (var output = new JarOutputStream(Files.newOutputStream(file))) {
			for (var entry : bytes.entrySet()) {
				output.putNextEntry(new JarEntry(entry.getKey().replace('.', '/') + ".class"));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
		return new HotModule(manager, "timerhot.Entry", file.toFile());
	}

	@Test
	void everyColdOnlineEntrypointRejectsAHotCaller() throws Exception {
		var conf = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		var app = new Application("TestTimerColdCaller" + conf.getServerId(), conf);
		new ProviderApp(app);
		var appBase = new TakeoverTestEnv.TestAppBase(app);
		app.initialize(appBase);
		var role = new GameOnline(appBase).getTimerRole();
		var account = new TimerAccount(new ArchOnline(appBase));
		var working = Files.createDirectories(directory.resolve("working"));
		var distribute = Files.createDirectories(directory.resolve("distribute"));
		var manager = new HotManager(appBase, working.toString(), distribute.toString());
		app.setHotManager(manager);
		try (var module = newModule(manager)) {
			var caller = module.loadClass("timerhot.Entry.ModuleEntry");
			assertSame(module, caller.getClassLoader());
			var call = caller.getMethod("call", TimerRole.class, TimerAccount.class, TimerScope.class,
					Class.class, int.class, boolean.class);
			var roleScope = new RoleTimers(role, 1).online();
			var accountScope = new AccountTimers(account, "account", "client").online();
			var assertions = new ArrayList<Executable>();
			for (boolean accountFamily : new boolean[]{false, true}) {
				for (int i = 0; i < ROLE_CALLS.size(); i++) {
					int entry = i;
					assertions.add(() -> {
						var failure = assertThrows(InvocationTargetException.class,
								() -> call.invoke(null, role, account, accountFamily ? accountScope : roleScope,
										Handle.class, entry, accountFamily));
						var rejected = assertInstanceOf(IllegalStateException.class, failure.getCause(),
								"account=" + accountFamily + " entry=" + entry);
						assertEquals("caller must not hot.", rejected.getMessage(),
								"account=" + accountFamily + " entry=" + entry);
					});
				}
			}
			assertAll(assertions);
		} finally {
			app.setHotManager(null);
			manager.stop();
			app.stop();
		}
	}
}
