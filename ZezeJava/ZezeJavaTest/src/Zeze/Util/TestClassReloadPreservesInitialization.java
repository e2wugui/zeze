package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestClassReloadPreservesInitialization {
	private static String source(String className, String key, int version) {
		return "package Zeze.Util; public class " + className.substring(className.lastIndexOf('.') + 1)
				+ " { public static int version; static { version = " + version
				+ "; System.setProperty(\"" + key + "\", \"" + version
				+ "\"); } public static int read() { return version; } }";
	}

	@ParameterizedTest
	@CsvSource({"false,false", "true,false", "false,true", "true,true"})
	public void firstActiveUseInitializesTheRedefinedVersion(boolean batch, boolean defaultLoader) throws Exception {
		var names = new LinkedHashMap<String, String>();
		var oldSources = new LinkedHashMap<String, String>();
		var newSources = new LinkedHashMap<String, String>();
		for (int i = 0; i < (batch ? 2 : 1); i++) {
			var id = UUID.randomUUID().toString().replace("-", "");
			var name = "Zeze.Util.ColdReloadTarget" + id;
			var key = "TestClassReloadPreservesInitialization." + id;
			names.put(name, key);
			oldSources.put(name, source(name, key, 1));
			newSources.put(name, source(name, key, 2));
		}
		try {
			var compiler = new InMemoryJavaCompiler();
			var originalBytes = compiler.compileAllToByteCode(oldSources);
			var classes = new LinkedHashMap<String, Class<?>>();
			var lookup = MethodHandles.privateLookupIn(ClassReloader.class, MethodHandles.lookup());
			for (var name : names.keySet())
				classes.put(name, defaultLoader ? lookup.defineClass(originalBytes.get(name)) : compiler.defineCompiled(name));
			for (var key : names.values())
				assertNull(System.getProperty(key), "defining a class must not initialize it");
			var replacementBytes = new InMemoryJavaCompiler().compileAllToByteCode(newSources);
			@Nullable ClassLoader loader = defaultLoader ? null : compiler.getClassloader();
			if (batch)
				ClassReloader.reloadClasses(replacementBytes.values(), loader);
			else
				ClassReloader.reloadClass(replacementBytes.values().iterator().next(), loader);
			assertAll(names.values().stream().map(key -> () ->
					assertNull(System.getProperty(key), "reloading must not run the old initializer")));
			for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
				assertEquals(2, entry.getValue().getMethod("read").invoke(null));
				assertEquals("2", System.getProperty(names.get(entry.getKey())));
			}
		} finally {
			for (var key : names.values())
				System.clearProperty(key);
		}
	}
}
