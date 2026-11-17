package Zeze.Hot;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestInvalidInterfaceRestoresPublishedJar {
	@TempDir
	Path directory;

	@Test
	void corruptInterfaceReturnsToStagingAndRestoresOldJar() throws Exception {
		var working = Files.createDirectories(directory.resolve("work"));
		var interfaces = Files.createDirectories(working.resolve("interfaces"));
		Files.createDirectories(working.resolve("modules"));
		var staging = Files.createDirectories(directory.resolve("staging"));
		var published = interfaces.resolve("restore.Entry.interface.jar");
		try (var jar = new JarOutputStream(Files.newOutputStream(published))) {
			jar.putNextEntry(new ZipEntry("marker"));
			jar.write(new byte[]{42});
		}
		var original = Files.readAllBytes(published);
		var corrupt = staging.resolve(published.getFileName());
		Files.write(corrupt, new byte[]{1, 2, 3});
		Files.write(staging.resolve("restore.Entry.jar"), new byte[0]);
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("restoreInterface", config);
		var manager = new HotManager(new AppBase() {
			@Override
			public Application getZeze() { return app; }
		}, working.toString(), staging.toString());
		var txn = new HotTransaction("restoreInterface");
		try {
			var install = HotManager.class.getDeclaredMethod("_install", String.class, HotTransaction.class);
			install.setAccessible(true);
			var failure = assertThrows(InvocationTargetException.class,
					() -> install.invoke(manager, "restore.Entry", txn));
			assertInstanceOf(java.util.zip.ZipException.class, failure.getCause());
			txn.rollback();
			assertArrayEquals(original, Files.readAllBytes(published));
			assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(corrupt));
			assertFalse(Files.exists(interfaces.resolve("restore.Entry.interface.jar.backup")));
		} finally {
			txn.rollback();
			var jarsField = HotManager.class.getDeclaredField("jars");
			jarsField.setAccessible(true);
			for (var jar : ((java.util.Map<?, ?>)jarsField.get(manager)).values())
				((JarFile)jar).close();
			manager.stop();
			app.stop();
		}
	}
}
