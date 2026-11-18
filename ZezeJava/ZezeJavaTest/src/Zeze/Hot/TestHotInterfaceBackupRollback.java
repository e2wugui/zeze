package Zeze.Hot;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
@Isolated("HotManager installs the process-wide BeanFactory application")
public class TestHotInterfaceBackupRollback {
	@TempDir
	Path directory;

	@Test
	void restoresTheOldInterfaceIndexWhenBackupCannotBeRemoved() throws Exception {
		var working = Files.createDirectory(directory.resolve("working"));
		var staging = Files.createDirectory(directory.resolve("staging"));
		var interfaces = Files.createDirectory(working.resolve("interfaces"));
		var oldJar = interfaces.resolve("Example.interface.jar");
		try (var jar = new JarOutputStream(Files.newOutputStream(oldJar))) {
			jar.putNextEntry(new ZipEntry("Example/Interface.class"));
		}
		var backup = Files.createDirectory(interfaces.resolve("Example.interface.jar.backup"));
		Files.createFile(backup.resolve("occupied"));
		Files.createFile(staging.resolve("Example.interface.jar"));
		Files.createFile(staging.resolve("Example.jar"));

		var applicationField = BeanFactory.class.getDeclaredField("zeze");
		applicationField.setAccessible(true);
		var previousApplication = applicationField.get(null);
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		config.setDefaultTableConf(new Config.TableConf());
		var app = new Application("TestHotInterfaceBackupRollback", config);
		try {
			var manager = new HotManager(new AppBase() {
				@Override
				public Application getZeze() {
					return app;
				}
			}, working.toString(), staging.toString());
			var closeJar = HotManager.class.getDeclaredMethod("closeJar", File.class);
			closeJar.setAccessible(true);
			try {
				var indexField = HotManager.class.getDeclaredField("zipEntries");
				indexField.setAccessible(true);
				var index = (Map<?, ?>)indexField.get(manager);
				assertTrue(index.containsKey("Example.Interface"));
				var install = HotManager.class.getDeclaredMethod("_install", String.class, HotTransaction.class);
				install.setAccessible(true);
				var transaction = new HotTransaction("interface backup rollback");
				var error = assertThrows(InvocationTargetException.class,
						() -> install.invoke(manager, "Example", transaction));
				assertInstanceOf(DirectoryNotEmptyException.class, error.getCause());
				transaction.rollback();
				assertTrue(index.containsKey("Example.Interface"));
			} finally {
				closeJar.invoke(manager, oldJar.toFile().getCanonicalFile());
			}
		} finally {
			applicationField.set(null, previousApplication);
		}
	}
}
