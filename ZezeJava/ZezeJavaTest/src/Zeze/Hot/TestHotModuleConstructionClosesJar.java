package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHotModuleConstructionClosesJar {
	private static final ThreadLocal<JarFile> opened = new ThreadLocal<>();

	private static final class ObservedModule extends HotModule {
		ObservedModule(File file) throws Exception {
			super(null, "missing.Entry", file);
		}

		@Override
		JarFile getJarFile() throws IOException {
			var jar = super.getJarFile();
			opened.set(jar);
			return jar;
		}
	}

	@TempDir
	Path directory;

	@Test
	void missingModuleClosesJar() throws Exception {
		checkFailure(false, ClassNotFoundException.class);
	}

	@Test
	void invalidModuleClosesJar() throws Exception {
		checkFailure(true, ClassFormatError.class);
	}

	private void checkFailure(boolean malformed, Class<? extends Throwable> failure) throws Exception {
		var path = directory.resolve("module.jar");
		try (var jar = new JarOutputStream(Files.newOutputStream(path))) {
			if (malformed) {
				jar.putNextEntry(new ZipEntry("missing/Entry/ModuleEntry.class"));
				jar.write(new byte[]{0, 1, 2});
			}
		}
		try {
			assertThrows(failure, () -> new ObservedModule(path.toFile()));
			var jar = opened.get();
			assertNotNull(jar);
			assertThrows(IllegalStateException.class, jar::size);
			Files.delete(path);
		} finally {
			var jar = opened.get();
			opened.remove();
			if (jar != null)
				jar.close();
		}
	}
}
