package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHotModuleClassIdentity {
	private static final String REDIRECT = "Redirect_hotidentity_ModuleFoo";
	private static final String SCHEMAS = "hotidentity.Schemas";
	@TempDir
	Path directory;

	@Test
	void repeatedRedirectLookupReturnsTheAlreadyDefinedClass() throws Exception {
		try (var module = newModule()) {
			var first = module.findRedirectClass(REDIRECT);
			assertSame(module, first.getClassLoader());
			assertSame(first, module.findRedirectClass(REDIRECT));
			assertSame(first, module.loadClass(REDIRECT));
		}
	}

	@Test
	void repeatedSchemaLookupUsesTheSameClassIdentity() throws Exception {
		try (var module = newModule()) {
			var first = module.loadClass(SCHEMAS, true);
			assertSame(module, first.getClassLoader());
			assertSame(first, module.loadClass(SCHEMAS, true));
			assertSame(first, module.findRedirectClass(SCHEMAS));
		}
	}

	@Test
	void packagedLookupAndCompiledDefinitionShareOnePublicationBoundary() throws Exception {
		try (var module = newModule(); var executor = Executors.newFixedThreadPool(8)) {
			byte[] byteCode;
			try (var input = module.getJarFile().getInputStream(module.getJarFile().getEntry(REDIRECT + ".class"))) {
				byteCode = input.readAllBytes();
			}
			var arrived = new CountDownLatch(8);
			var start = new CountDownLatch(1);
			var futures = new ArrayList<Future<Class<?>>>();
			try {
				for (int i = 0; i < 8; i++) {
					boolean packaged = (i & 1) == 0;
					futures.add(executor.submit(() -> {
						arrived.countDown();
						assertTrue(start.await(10, TimeUnit.SECONDS));
						return packaged ? module.findRedirectClass(REDIRECT) : module.defineRedirectClass(REDIRECT, byteCode);
					}));
				}
				assertTrue(arrived.await(10, TimeUnit.SECONDS));
			} finally {
				start.countDown();
			}
			var first = futures.getFirst().get(10, TimeUnit.SECONDS);
			for (var future : futures)
				assertSame(first, future.get(10, TimeUnit.SECONDS));
		}
	}

	private HotModule newModule() throws Exception {
		var bytes = new InMemoryJavaCompiler().compileAllToByteCode(Map.of(
				REDIRECT, "public class " + REDIRECT + " {}",
				SCHEMAS, "package hotidentity; public class Schemas {}"));
		var file = directory.resolve("module.jar");
		try (var output = new JarOutputStream(Files.newOutputStream(file))) {
			for (var entry : bytes.entrySet()) {
				output.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
		return new HotModule(file.toFile(), SCHEMAS);
	}
}
