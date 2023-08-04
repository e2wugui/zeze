package Zeze.Hot;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Distribute {
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void main(String [] args) throws Exception {
		// 搜索classes目录，自动识别Module并打包。
		// 每个Module打成两个包。一个interface，一个其他。
		// Module除外的打成一个包，不热更。可能有例外需要处理。
		// todo 例外，下面的类放在了全局空间，需要打入相应的模块：
		// build zezex 的时候，redirect.class生成到build目录了，这个不是即时编译，仅在内存的吗？
		// 是不是zezex特殊处理了（好像是）。

		var classes = "build/classes/java/main";
		var workingDir = "hot";
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-classes"))
				classes = args[++i];
			else if (args[i].equals("-workingDir"))
				workingDir = args[++i];
		}
		Path.of(workingDir, "interfaces").toFile().mkdirs();
		Path.of(workingDir, "modules").toFile().mkdirs();

		var home = Path.of(classes);
		var packages = new ArrayList<Package>();
		packages.add(new Package(home.toFile()));
		pack(home, workingDir, home, packages);
		assert packages.size() == 1;
		packages.get(0).pack(home, workingDir);
	}

	public static class Package {
		private final File dir;
		private final Set<File> classes = new HashSet<>();

		public Package(File dir) {
			this.dir = dir;
		}

		public void add(File file) {
			classes.add(file);
		}

		public void pack(Path home, String workingDir) throws Exception {
			var module = home.relativize(dir.toPath()).toString()
					.replace("\\", "/")
					.replace("/", ".");

			var interfaceManifest = new Manifest();
			var moduleManifest = new Manifest();
			if (module.isEmpty()) {
				// todo server.jar，需要得到项目的名字。
				var serverJarFile = Path.of(workingDir, "server.jar").toFile();
				try (var serverJar = new JarOutputStream(new FileOutputStream(serverJarFile), interfaceManifest)) {
					for (var file : classes) {
						var classFile = home.relativize(file.toPath()).toString().replace("\\", "/");
						var entry = new ZipEntry(classFile);
						entry.setTime(file.lastModified());
						serverJar.putNextEntry(entry);
						serverJar.write(Files.readAllBytes(file.toPath()));
					}
				}

				return; // done;
			}

			var interfaceJarFile = Path.of(workingDir, "interfaces", module + ".interface.jar").toFile();
			var moduleJarFile = Path.of(workingDir, "modules", module + ".jar").toFile();
			try (var interfaceJar = new JarOutputStream(new FileOutputStream(interfaceJarFile), interfaceManifest);
				 var moduleJar = new JarOutputStream(new FileOutputStream(moduleJarFile), moduleManifest)
				) {
				for (var file : classes) {
					var classFile = home.relativize(file.toPath()).toString().replace("\\", "/");
					var className = classFile.replace("/", ".");
					className = className.substring(0, className.indexOf(".class")); // remove ".class"
					var entry = new ZipEntry(classFile);
					entry.setTime(file.lastModified());
					if (Class.forName(className).isInterface()) {
						interfaceJar.putNextEntry(entry);
						interfaceJar.write(Files.readAllBytes(file.toPath()));
					} else {
						moduleJar.putNextEntry(entry);
						moduleJar.write(Files.readAllBytes(file.toPath()));
					}
				}
			}
		}

		@Override
		public String toString() {
			return dir.toString() + "=" + classes.size();
		}
	}

	public static void pack(Path home, String workingDir, Path dir, ArrayList<Package> packages) throws Exception {
		var files = dir.toFile().listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.isDirectory()) {
				var module = isModuleDir(file);
				if (module)
					packages.add(new Package(file));
				pack(home, workingDir, file.toPath(), packages);
				if (module) {
					var doneModule = packages.remove(packages.size() - 1);
					doneModule.pack(home, workingDir);
				}
				continue;
			}
			var lastPackage = packages.get(packages.size() - 1);
			lastPackage.add(file);
		}
	}

	public static boolean isModuleDir(File file) {
		var moduleFile = new File(file, "Module" + file.getName() + ".class");
		return moduleFile.exists();
	}
}
