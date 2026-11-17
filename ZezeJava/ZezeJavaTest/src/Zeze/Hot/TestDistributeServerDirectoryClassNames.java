package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Provider.BModule;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDistributeServerDirectoryClassNames {
	@TempDir
	Path directory;

	@Test
	void classDirectoryLoadsBeansAndIgnoresResources() throws Exception {
		var packageDir = Files.createDirectories(directory.resolve("Zeze/Builtin/Provider"));
		// 扫描器通过运行 classpath 装载；此处只构建真实的目录条目。
		Files.write(packageDir.resolve("BModule.class"), new byte[0]);
		Files.writeString(packageDir.resolve("metadata.txt"), "resource");
		var solution = DistributeServer.class.getDeclaredField("solution");
		solution.setAccessible(true);
		var previous = solution.get(null);
		var beans = DistributeServer.getBeans();
		var previousBean = beans.get(BModule.class.getName());
		try {
			solution.set(null, "Zeze.Builtin.Provider.");
			var scan = DistributeServer.class.getDeclaredMethod("loadBean", Path.class, java.io.File.class);
			scan.setAccessible(true);
			scan.invoke(null, directory, directory.toFile());
			assertInstanceOf(BModule.class, beans.get(BModule.class.getName()));
		} finally {
			solution.set(null, previous);
			if (previousBean != null)
				beans.put(BModule.class.getName(), previousBean);
			else
				beans.remove(BModule.class.getName());
		}
	}
}
