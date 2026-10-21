package Zeze.Hot;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated("HotManager installs the process-wide BeanFactory application")
public class TestHotManagerDirectoryLayout {
	@TempDir
	Path tempDir;
	private @Nullable Field applicationField;
	private @Nullable Object previousApplication;

	@BeforeEach
	void setUp() throws Exception {
		applicationField = BeanFactory.class.getDeclaredField("zeze");
		applicationField.setAccessible(true);
		previousApplication = applicationField.get(null);
		Files.createDirectories(tempDir.resolve("working/interfaces"));
		Files.createDirectories(tempDir.resolve("working/modules"));
		Files.createDirectories(tempDir.resolve("working/alias"));
		Files.createDirectories(tempDir.resolve("alias"));
	}

	@AfterEach
	void tearDown() throws Exception {
		Objects.requireNonNull(applicationField).set(null, previousApplication);
	}

	private static HotManager createManager(Path working, Path staging) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setDefaultTableConf(new Config.TableConf());
		var app = new Application("TestHotManagerDirectoryLayout", conf);
		return new HotManager(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		}, working.toString(), staging.toString());
	}

	private void assertRejectedStaging(String activeDirectory) {
		var working = tempDir.resolve("working");
		var staging = working.resolve("alias/../" + activeDirectory);
		var error = assertThrows(RuntimeException.class, () -> createManager(working, staging));
		assertTrue(error.getMessage().contains("distributeDir is sub-dir"));
	}

	@Test
	void rejectsStagingInsideInterfacesThroughDotSegments() {
		assertRejectedStaging("interfaces");
	}

	@Test
	void rejectsStagingInsideModulesThroughDotSegments() {
		assertRejectedStaging("modules");
	}

	@Test
	void rejectsStagingAncestorThroughDotSegments() {
		var error = assertThrows(RuntimeException.class,
				() -> createManager(tempDir.resolve("working"), tempDir.resolve("alias/..")));
		assertTrue(error.getMessage().contains("workingDir is sub-dir"));
	}

	@Test
	void rejectsDirectOverlap() {
		var working = tempDir.resolve("working");
		assertThrows(RuntimeException.class, () -> createManager(working, working.resolve("interfaces")));
		assertThrows(RuntimeException.class, () -> createManager(working, working.resolve("modules")));
	}

	@Test
	void acceptsSiblingStagingWithEquivalentWorkingPath() throws Exception {
		var staging = Files.createDirectory(tempDir.resolve("staging"));
		assertNotNull(createManager(tempDir.resolve("working/alias/.."), staging));
	}
}
