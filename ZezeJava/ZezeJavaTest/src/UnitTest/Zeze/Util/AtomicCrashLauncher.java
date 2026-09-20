package UnitTest.Zeze.Util;

import Zeze.Util.AtomicFileWriter;

import java.nio.file.Path;

/**
 * 崩溃演员：按剧情halt(137)（不走shutdown hook、不flush），最接近断电的进程内死亡。
 */
public final class AtomicCrashLauncher {
	public static void main(String[] args) throws Exception {
		var target = Path.of(args[0]);
		var out = AtomicFileWriter.openOutput(target);
		out.write("new-complete-version".getBytes());
		if (args.length > 1 && args[1].equals("halt-before-close"))
			Runtime.getRuntime().halt(137); // 写途中死亡：temp可能空/半截，目标必是旧版
		out.close(); // 换版生效后死亡：目标必是新版
		Runtime.getRuntime().halt(137);
	}
}
