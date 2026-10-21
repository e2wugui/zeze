package Zeze.Hot;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Objects;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.Net.Binary;
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
public class TestDistributeDigestSlice {
	@TempDir
	Path tempDir;
	private @Nullable Field applicationField;
	private @Nullable Object previousApplication;
	private @Nullable DistributeManager manager;

	@BeforeEach
	void setUp() throws Exception {
		applicationField = BeanFactory.class.getDeclaredField("zeze");
		applicationField.setAccessible(true);
		previousApplication = applicationField.get(null);
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setDefaultTableConf(new Config.TableConf());
		var app = new Application("TestDistributeDigestSlice", conf);
		var appBase = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
		var working = tempDir.resolve("working");
		Files.createDirectories(working.resolve("interfaces"));
		Files.createDirectories(working.resolve("modules"));
		var staging = Files.createDirectory(tempDir.resolve("staging"));
		manager = new HotManager(appBase, working.toString(), staging.toString()).getDistributeManager();
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (manager != null)
				manager.closeAll();
		} finally {
			Objects.requireNonNull(applicationField).set(null, previousApplication);
		}
	}

	@Test
	void acceptsDigestInsideLargerBackingArray() throws Exception {
		var dm = Objects.requireNonNull(manager);
		var payload = new Binary("module payload");
		dm.open("module.jar").append(0, payload);
		var digest = MessageDigest.getInstance("MD5").digest(payload.toBytes());
		var backing = new byte[digest.length + 7];
		System.arraycopy(digest, 0, backing, 3, digest.length);
		var slice = new Binary(backing, 3, digest.length);
		assertTrue(dm.closeAndVerify("module.jar", slice));
		assertTrue(dm.closeAndVerify("module.jar", slice));
		dm.commitDistribute();
		assertTrue(Files.exists(tempDir.resolve("staging/ready")));
	}

	@Test
	void rejectsTruncatedViewEvenWhenBackingArrayIsCorrectDigest() throws Exception {
		var dm = Objects.requireNonNull(manager);
		var payload = new Binary("module payload");
		dm.open("module.jar").append(0, payload);
		var digest = MessageDigest.getInstance("MD5").digest(payload.toBytes());
		var slice = new Binary(digest, 1, digest.length - 1);
		assertFalse(dm.closeAndVerify("module.jar", slice));
		assertFalse(dm.closeAndVerify("module.jar", slice));
		assertThrows(IOException.class, dm::commitDistribute);
		assertFalse(Files.exists(tempDir.resolve("staging/ready")));
	}

	@Test
	void acceptsFullDigestAndRejectsWrongContents() throws Exception {
		var dm = Objects.requireNonNull(manager);
		var payload = new Binary("module payload");
		dm.open("module.jar").append(0, payload);
		var digest = MessageDigest.getInstance("MD5").digest(payload.toBytes());
		assertTrue(dm.closeAndVerify("module.jar", new Binary(digest)));
		dm.open("wrong.jar").append(0, payload);
		digest[0] ^= 1;
		assertFalse(dm.closeAndVerify("wrong.jar", new Binary(digest)));
	}
}
