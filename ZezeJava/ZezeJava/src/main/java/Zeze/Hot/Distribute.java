package Zeze.Hot;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import Zeze.Arch.Gen.GenModule;

public class Distribute {
	static String classes = "build/classes/java/main";
	static String workingDir = "hot";
	static boolean exportBean = true;
	static Path home;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void main(String [] args) throws Exception {
		// 搜索classes目录，自动识别Module并打包。
		// 每个Module打成两个包。一个interface，一个其他。
		// Module除外的打成一个包，不热更。可能有例外需要处理。
		// 【例外】Redirect_生成的类放在了全局空间，需要打入相应的模块：

		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-classes":
				classes = args[++i];
				break;
			case "-workingDir":
				workingDir = args[++i];
				break;
			case "-privateBean":
				exportBean = false;
				break;
			}
		}
		Path.of(workingDir, "interfaces").toFile().mkdirs();
		Path.of(workingDir, "modules").toFile().mkdirs();

		home = Path.of(classes);
		var packages = new ArrayList<Package>();
		packages.add(new Package(home.toFile()));
		pack(home, packages);
		assert packages.size() == 1;
		packages.get(0).pack();
	}

	public static final HashMap<String, JarOutputStream> moduleJars = new HashMap<>();

	public static class Package {
		private final File dir;
		private final Set<File> classes = new HashSet<>();

		public Package(File dir) {
			this.dir = dir;
		}

		public void add(File file) {
			classes.add(file);
		}

		public void pack() throws Exception {
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
						if (classFile.indexOf('/') == -1 && classFile.startsWith(GenModule.REDIRECT_PREFIX)) {
							// 是Redirect生成的子类。
							// 解析名字；根据名字找到相应的module.jar；并打包进去。
							var moduleClassName = classFile.substring("Redirect_".length(), classFile.length() - ".class".length())
									.replace("_", ".");
							var moduleNamespace = moduleClassName.substring(0, moduleClassName.lastIndexOf('.'));
							var moduleJar = moduleJars.get(moduleNamespace);
							var entry = new ZipEntry(classFile);
							entry.setTime(file.lastModified());
							moduleJar.putNextEntry(entry);
							moduleJar.write(Files.readAllBytes(file.toPath()));

							continue;
						}
						var entry = new ZipEntry(classFile);
						entry.setTime(file.lastModified());
						serverJar.putNextEntry(entry);
						serverJar.write(Files.readAllBytes(file.toPath()));
					}
				}

				for (var e : moduleJars.entrySet()) {
					//System.out.println(e.getKey() + " ---- close");
					e.getValue().close();
				}
				moduleJars.clear();
				return; // done;
			}

			var interfaceJarFile = Path.of(workingDir, "interfaces", module + ".interface.jar").toFile();
			var moduleJarFile = Path.of(workingDir, "modules", module + ".jar").toFile();
			try (var interfaceJar = new JarOutputStream(new FileOutputStream(interfaceJarFile), interfaceManifest)) {
				// moduleJar 后面还可能添加文件，这里不关闭。
				var moduleJar = new JarOutputStream(new FileOutputStream(moduleJarFile), moduleManifest);
				moduleJars.put(module, moduleJar);
				var beanNames = new HashSet<String>();
				var logClasses = new ArrayList<LogEntry>();
				for (var file : classes) {
					var classFile = home.relativize(file.toPath()).toString().replace("\\", "/");
					var className = classFile.replace("/", ".");
					className = className.substring(0, className.indexOf(".class")); // remove ".class"
					var entry = new ZipEntry(classFile);
					entry.setTime(file.lastModified());
					var cls = Class.forName(className);
					if (Zeze.Transaction.Log.class.isAssignableFrom(cls)) {
						// log 收集下来，后面再确认它确实是Bean里面的Log才加入interface.jar。
						logClasses.add(new LogEntry(cls, entry, file));
					} else if (cls.isInterface() || (exportBean && (
							Zeze.Transaction.Bean.class.isAssignableFrom(cls)
							|| Zeze.Transaction.Data.class.isAssignableFrom(cls)
							|| Zeze.Transaction.BeanKey.class.isAssignableFrom(cls)
							|| Zeze.Arch.RedirectResult.class.isAssignableFrom(cls)
							))) {
						if (exportBean && Zeze.Transaction.Bean.class.isAssignableFrom(cls))
							beanNames.add(cls.getName()); // bean 收集下来，用来下一步判断log.class。
						interfaceJar.putNextEntry(entry);
						interfaceJar.write(Files.readAllBytes(file.toPath()));
					} else {
						moduleJar.putNextEntry(entry);
						moduleJar.write(Files.readAllBytes(file.toPath()));
					}
				}
				for (var e : logClasses) {
					if (e.isBeanLog(beanNames)) {
						interfaceJar.putNextEntry(e.entry);
						interfaceJar.write(Files.readAllBytes(e.file.toPath()));
					} else {
						moduleJar.putNextEntry(e.entry);
						moduleJar.write(Files.readAllBytes(e.file.toPath()));
					}
				}
			}
		}

		@Override
		public String toString() {
			return dir.toString() + "=" + classes.size();
		}
	}

	public static class LogEntry {
		public final Class<?> logClass;
		public final ZipEntry entry;
		public final File file;

		public boolean isBeanLog(HashSet<String> beanNames) {
			var logName = logClass.getName();
			var innerIdx = logName.indexOf('$');
			if (innerIdx == -1) // bean.log 肯定是内部类。
				return false;
			var beanName = logName.substring(0, innerIdx);
			return beanNames.contains(beanName);
		}

		public LogEntry(Class<?> logClass, ZipEntry entry, File file) {
			this.logClass = logClass;
			this.entry = entry;
			this.file = file;
		}
	}
	public static void pack(Path dir, ArrayList<Package> packages) throws Exception {
		var files = dir.toFile().listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.isDirectory()) {
				var module = isModuleDir(file);
				if (module)
					packages.add(new Package(file));
				pack(file.toPath(), packages);
				if (module) {
					var doneModule = packages.remove(packages.size() - 1);
					doneModule.pack();
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
