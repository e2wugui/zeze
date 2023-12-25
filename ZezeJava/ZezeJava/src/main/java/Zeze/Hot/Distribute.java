package Zeze.Hot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Builtin.Provider.BModule;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;

public class Distribute {
	private final String classesDir;
	private final Path classesHome;
	private final boolean exportBean;
	private final String workingDir;
	private Set<String> hotModules;
	private String projectName;
	private final String providerModuleBinds;
	private final String configXml;

	private final HashMap<String, JarOutputStream> hotModuleJars = new HashMap<>();
	private JarOutputStream projectJar; // 所有非热更代码都打包到这里。

	// 兼容测试。
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public Distribute(String classesDir,
					  boolean exportBean,
					  String workingDir,
					  String providerModuleBinds,
					  String configXml) {

		this.classesDir = classesDir;
		this.classesHome = Path.of(classesDir);
		this.exportBean = exportBean;
		this.workingDir = workingDir;
		this.providerModuleBinds = providerModuleBinds;
		this.configXml = configXml;

		Path.of(workingDir, "interfaces").toFile().mkdirs();
		Path.of(workingDir, "modules").toFile().mkdirs();
	}

	public void pack(Set<String> hotModules,
					 String projectName,
					 String solutionName) throws Exception {
		this.hotModules = hotModules;
		this.projectName = projectName;

		var packages = new ArrayList<Package>();
		packages.add(new Package(classesHome.toFile()));
		pack(classesHome, packages);
		assert packages.size() == 1;
		packages.get(0).pack();

		if (null != projectJar) {
			projectJar.close();
			projectJar = null;
		}

		var schemasManifest = new Manifest();
		var schemasJarFile = Path.of(workingDir, HotManager.SchemasPrefix + solutionName + HotManager.SchemasSuffix).toFile();
		try (var schemasJar = new JarOutputStream(new FileOutputStream(schemasJarFile), schemasManifest)) {
			var schemasFile = Path.of(classesDir, solutionName, "Schemas.class");
			var entry = new ZipEntry(solutionName + "/Schemas.class");
			entry.setTime(schemasFile.toFile().lastModified());
			schemasJar.putNextEntry(entry);
			var bytes = Files.readAllBytes(schemasFile);
			schemasJar.write(bytes);
		}

		if (!providerModuleBinds.isEmpty() && !configXml.isEmpty()) {
			var config = Config.load(configXml);
			if (new File(config.getHotWorkingDir()).exists()) {
				var appBase = (AppBase)Class.forName(solutionName + ".App").getConstructor().newInstance();
				appBase.createZeze(config);
				appBase.createService();
				var providerApp = new ProviderApp(appBase.getZeze());
				appBase.createModules();
				providerApp.buildProviderModuleBinds(ProviderModuleBinds.load(providerModuleBinds), appBase.getModules());
				// 打包配置到module.jar里面，前面的关闭需要移到后面。
				providerApp.modules.foreach((key, value) -> packModuleConfig(findModule(appBase, key), value));
			}
			else {
				System.out.println("hotWorkingDir not exist, please prepare and re-run distribute.");
			}
		} else {
			System.out.println("-providerModuleBinds or -config not present, skip module config.");
		}

		for (var e : hotModuleJars.entrySet()) {
			//System.out.println(e.getKey() + " ---- close");
			e.getValue().close();
		}
		hotModuleJars.clear();

		var hotAgents = new ArrayList<HotAgent>();
		for (var hotManager : hotManagers) {
			hotAgents.add(new HotAgent(hotManager));
		}
		for (var hotAgent : hotAgents) {
			hotAgent.distribute(new File(workingDir, "modules"));
			hotAgent.distribute(new File(workingDir, "interfaces"));
		}
		// HotManager 加上Ready阶段（保持锁定）；然后集中Commit即可实现原子发布。
		for (var hotAgent : hotAgents) {
			hotAgent.commit();
		}
	}

	private void packModuleConfig(IModule module, BModule.Data config) {
		try {
			var moduleJar = hotModuleJars.get(module.getFullName());
			if (null == moduleJar)
				return;

			var classFile = HotModule.eModuleConfigName;
			var entry = new ZipEntry(classFile);
			moduleJar.putNextEntry(entry);
			var bbConfig = ByteBuffer.Allocate();
			config.encode(bbConfig);
			moduleJar.write(bbConfig.Bytes, bbConfig.ReadIndex, bbConfig.size());
		} catch (Exception ex) {
			Task.forceThrow(ex);
		}
	}

	private static IModule findModule(AppBase appBase, int moduleId) {
		for (var module : appBase.getModules().values()) {
			if (module.getId() == moduleId)
				return module;
		}
		return null;
	}

	private static final ArrayList<String> hotManagers = new ArrayList<String>();

	public static void main(String [] args) throws Exception {
		var classesDir = "build/classes/java/main";
		var exportBean = true;
		var workingDir = "hot";
		var app = "";
		var providerModuleBinds = "";
		var configXml = "";

		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-classes":
				classesDir = args[++i];
				break;
			case "-workingDir":
				workingDir = args[++i];
				break;
			case "-privateBean":
				exportBean = false;
				break;
			case "-app":
				app = args[++i];
				break;
			case "-providerModuleBinds":
				providerModuleBinds = args[++i];
				break;
			case "-config":
				configXml = args[++i];
				break;
			default:
				hotManagers.add(args[i]);
				break;
			}
		}

		var distribute = new Distribute(
				classesDir, exportBean, workingDir,
				providerModuleBinds, configXml);

		var appClass = Class.forName(app);
		var method = appClass.getMethod("distributeHot", Distribute.class);
		method.invoke(null, distribute);
	}

	public boolean isHotModule(String moduleNamespace) {
		return null == hotModules || hotModules.contains(moduleNamespace);
	}

	public JarOutputStream getProjectJar() throws IOException {
		if (null == projectJar) {
			var manifest = new Manifest();
			var serverJarFile = Path.of(workingDir, projectName + ".jar").toFile();
			projectJar = new JarOutputStream(new FileOutputStream(serverJarFile), manifest);
		}
		return projectJar;
	}

	public class Package {
		private final File dir;
		private final Set<File> classes = new HashSet<>();

		public Package(File dir) {
			this.dir = dir;
		}

		public void add(File file) {
			classes.add(file);
		}

		public void pack() throws Exception {
			var module = classesHome.relativize(dir.toPath()).toString()
					.replace("\\", "/")
					.replace("/", ".");

			var interfaceManifest = new Manifest();
			var moduleManifest = new Manifest();
			if (module.isEmpty() || !isHotModule(module)) {
				var serverJar = getProjectJar();
				for (var file : classes) {
					var classFile = classesHome.relativize(file.toPath())
							.toString().replace("\\", "/");

					// 检查是否 Redirect 生成的子类。
					// 解析名字；根据名字以及类型决定是否打包进hotModule.jar。
					if (classFile.indexOf('/') == -1 && classFile.startsWith(GenModule.REDIRECT_PREFIX)) {
						var moduleClassName = classFile.substring(
								"Redirect_".length(), classFile.length() - ".class".length())
								.replace("_", ".");
						var moduleNamespace = moduleClassName.substring(0, moduleClassName.lastIndexOf('.'));
						if (isHotModule(moduleNamespace)) {
							var moduleJar = hotModuleJars.get(moduleNamespace);
							var entry = new ZipEntry(classFile);
							entry.setTime(file.lastModified());
							moduleJar.putNextEntry(entry);
							moduleJar.write(Files.readAllBytes(file.toPath()));
							continue;
						}
						// else 加入projectJar
					}
					var entry = new ZipEntry(classFile);
					entry.setTime(file.lastModified());
					serverJar.putNextEntry(entry);
					serverJar.write(Files.readAllBytes(file.toPath()));
				}

				return; // projectJar done;
			}

			// 打包热更模块, HotModule
			var interfaceJarFile = Path.of(workingDir, "interfaces", module + ".interface.jar").toFile();
			var moduleJarFile = Path.of(workingDir, "modules", module + ".jar").toFile();
			try (var interfaceJar = new JarOutputStream(new FileOutputStream(interfaceJarFile), interfaceManifest)) {
				// moduleJar 后面还可能添加文件，这里不关闭。
				var moduleJar = new JarOutputStream(new FileOutputStream(moduleJarFile), moduleManifest);
				hotModuleJars.put(module, moduleJar);
				var beanNames = new HashSet<String>();
				var logClasses = new ArrayList<PackEntry>();
				var beanReadonlyMaybe = new ArrayList<PackEntry>();
				for (var file : classes) {
					var classFile = classesHome.relativize(file.toPath()).toString().replace("\\", "/");
					var className = classFile.replace("/", ".");
					className = className.substring(0, className.indexOf(".class")); // remove ".class"
					var entry = new ZipEntry(classFile);
					entry.setTime(file.lastModified());
					var cls = Class.forName(className);
					if (Zeze.Transaction.Log.class.isAssignableFrom(cls)) {
						// log 收集下来，后面再确认它确实是Bean里面的Log才加入interface.jar。
						logClasses.add(new PackEntry(cls, entry, file));
						// bean's ReadOnly interface 也需要跟随bean一起打包。
					} else if (cls.isInterface()) {
						if (!cls.getName().endsWith("ReadOnly")) {
							interfaceJar.putNextEntry(entry);
							interfaceJar.write(Files.readAllBytes(file.toPath()));
						} else {
							// 怀疑是Bean的生成的interface，先保存下来。
							beanReadonlyMaybe.add(new PackEntry(cls, entry, file));
						}
					}
					else if (exportBean && (
									Zeze.Transaction.Bean.class.isAssignableFrom(cls)
								|| Zeze.Transaction.Data.class.isAssignableFrom(cls)
								|| Zeze.Transaction.BeanKey.class.isAssignableFrom(cls)
								|| Zeze.Arch.RedirectResult.class.isAssignableFrom(cls))) {
						if (Zeze.Transaction.Bean.class.isAssignableFrom(cls))
							beanNames.add(cls.getName()); // bean 收集下来，用来下一步判断log.class。
						interfaceJar.putNextEntry(entry);
						interfaceJar.write(Files.readAllBytes(file.toPath()));
					} else {
						if (Zeze.Transaction.Bean.class.isAssignableFrom(cls))
							beanNames.add(cls.getName()); // bean 收集下来，用来下一步判断log.class。
						moduleJar.putNextEntry(entry);
						moduleJar.write(Files.readAllBytes(file.toPath()));
					}
				}
				for (var e : logClasses) {
					if (exportBean && e.isBeanLog(beanNames)) {
						// 确实是Bean的Log类，
						interfaceJar.putNextEntry(e.entry);
						interfaceJar.write(Files.readAllBytes(e.file.toPath()));
					} else {
						moduleJar.putNextEntry(e.entry);
						moduleJar.write(Files.readAllBytes(e.file.toPath()));
					}
				}
				// 使用名字更精确的判断是否是Bean的生成的interface。
				for (var e : beanReadonlyMaybe) {
					var clsName = e.class1.getName();
					var beanName = clsName.substring(0, clsName.length() - "ReadOnly".length());
					if (!exportBean & beanNames.contains(beanName)) {
						moduleJar.putNextEntry(e.entry);
						moduleJar.write(Files.readAllBytes(e.file.toPath()));
					} else {
						interfaceJar.putNextEntry(e.entry);
						interfaceJar.write(Files.readAllBytes(e.file.toPath()));
					}
				}
			}
		}

		@Override
		public String toString() {
			return dir.toString() + "=" + classes.size();
		}
	}

	public static class PackEntry {
		public final Class<?> class1;
		public final ZipEntry entry;
		public final File file;

		public boolean isBeanLog(HashSet<String> beanNames) {
			var logName = class1.getName();
			var innerIdx = logName.indexOf('$');
			if (innerIdx == -1) // bean.log 肯定是内部类。
				return false;
			var beanName = logName.substring(0, innerIdx);
			return beanNames.contains(beanName);
		}

		public PackEntry(Class<?> logClass, ZipEntry entry, File file) {
			this.class1 = logClass;
			this.entry = entry;
			this.file = file;
		}
	}
	public void pack(Path dir, ArrayList<Package> packages) throws Exception {
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
