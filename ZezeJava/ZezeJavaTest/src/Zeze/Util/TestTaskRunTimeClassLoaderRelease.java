package Zeze.Util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.Map;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Fast
public class TestTaskRunTimeClassLoaderRelease {
	private static WeakReference<ClassLoader> recordRunTime(Class<?> contract, boolean named) {
		var loader = new ClassLoader(TestTaskRunTimeClassLoaderRelease.class.getClassLoader()) { };
		var payload = Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> 0L);
		TaskSpec<?> spec;
		if (payload instanceof Action0 action)
			spec = TaskSpec.ofAction(action);
		else if (payload instanceof FuncLong func)
			spec = TaskSpec.ofFunc(func);
		else
			spec = TaskSpec.ofFunc0((Func0<?>)payload);
		if (named)
			spec.name("TestTaskRunTimeClassLoaderRelease.named");
		spec.call();
		return new WeakReference<>(loader);
	}

	private static boolean hasLiveLoader(Map<String, WeakReference<ClassLoader>> loaders) {
		return loaders.values().stream().anyMatch(reference -> reference.get() != null);
	}

	@Test
	public void recordedPayloadsDoNotKeepTheirClassLoadersAlive() throws InterruptedException {
		assumeTrue(ZezeCounter.ENABLE, "runtime statistics must be enabled to exercise observer caching");
		var loaders = Map.of(
				"action class", recordRunTime(Action0.class, false),
				"function class", recordRunTime(FuncLong.class, false),
				"value function class", recordRunTime(Func0.class, false),
				"named action", recordRunTime(Action0.class, true));
		for (int attempt = 0; attempt < 80 && hasLiveLoader(loaders); attempt++) {
			System.gc();
			Thread.sleep(25);
		}
		assertAll(loaders.entrySet().stream().map(entry -> () ->
				assertNull(entry.getValue().get(), entry.getKey() + " loader is still retained")));
	}
}
