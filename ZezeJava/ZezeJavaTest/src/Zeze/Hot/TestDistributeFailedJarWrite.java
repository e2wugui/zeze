package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;
import Zeze.Util.AtomicOutputFile;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDistributeFailedJarWrite {
	@TempDir Path home;

	@Test
	public void missingSchemasClassPreservesThePublishedSchemasJar() throws Exception {
		var classes = Files.createDirectory(home.resolve("classes"));
		var working = Files.createDirectory(home.resolve("working"));
		var old = new byte[]{1, 2, 3};
		var target = working.resolve(HotManager.SchemasPrefix + "Solution" + HotManager.SchemasSuffix);
		AtomicOutputFile.writeAtomically(target, old);
		var distribute = new Distribute(classes.toString(), true, working.toString(), "", "", false);
		assertThrows(Exception.class, () -> distribute.pack(Set.of(), "Project", "Solution"));
		assertArrayEquals(old, Files.readAllBytes(target));
		assertNoTemps(working);
	}

	@Test
	public void classLoadingFailurePreservesBothExistingModuleJars() throws Exception {
		var classes = Files.createDirectory(home.resolve("classes"));
		var module = Files.createDirectories(classes.resolve("missing/Module"));
		AtomicOutputFile.writeAtomically(module.resolve("ModuleModule.class"), new byte[]{0});
		var working = Files.createDirectory(home.resolve("working"));
		var distribute = new Distribute(classes.toString(), true, working.toString(), "", "", false);
		var old = new byte[]{4, 5, 6};
		var intf = working.resolve("interfaces/missing.Module.interface.jar");
		var impl = working.resolve("modules/missing.Module.jar");
		AtomicOutputFile.writeAtomically(intf, old);
		AtomicOutputFile.writeAtomically(impl, old);
		assertThrows(ClassNotFoundException.class,
				() -> distribute.pack(Set.of("missing.Module"), "Project", "Solution"));
		assertArrayEquals(old, Files.readAllBytes(intf));
		assertArrayEquals(old, Files.readAllBytes(impl));
		assertNoTemps(working);
	}

	@Test
	public void successfulPackPublishesCompleteProjectAndSchemasJars() throws Exception {
		var classes = Files.createDirectory(home.resolve("classes"));
		var schemas = Files.createDirectories(classes.resolve("Solution")).resolve("Schemas.class");
		var bytes = new byte[]{7, 8, 9};
		Files.write(schemas, bytes);
		var working = Files.createDirectory(home.resolve("working"));
		var projectJar = working.resolve("Project.jar");
		var schemasJar = working.resolve(HotManager.SchemasPrefix + "Solution" + HotManager.SchemasSuffix);
		AtomicOutputFile.writeAtomically(projectJar, new byte[]{1});
		AtomicOutputFile.writeAtomically(schemasJar, new byte[]{2});
		var distribute = new Distribute(classes.toString(), true, working.toString(), "", "", false);
		distribute.pack(Set.of(), "Project", "Solution");
		for (var target : new Path[]{projectJar, schemasJar}) {
			try (var jar = new JarFile(target.toFile());
				 var entry = jar.getInputStream(jar.getJarEntry("Solution/Schemas.class"))) {
				assertArrayEquals(bytes, entry.readAllBytes());
			}
		}
		assertNoTemps(working);
	}

	private static void assertNoTemps(Path dir) throws Exception {
		try (var files = Files.walk(dir)) {
			assertFalse(files.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
		}
	}
}
