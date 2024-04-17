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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Builtin.HotDistribute.Commit;
import Zeze.Builtin.HotDistribute.TryDistribute;
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
	private final boolean atomicAll;

	private final HashMap<String, JarOutputStream> hotModuleJars = new HashMap<>();
	private JarOutputStream projectJar; // 所有非热更代码都打包到这里。

	// 兼容测试。
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public Distribute(String classesDir,
					  boolean exportBean,
					  String workingDir,
					  String providerModuleBinds,
					  String configXml,
					  boolean atomicAll) {

		this.classesDir = classesDir;
		this.classesHome = Path.of(classesDir);
		this.exportBean = exportBean;
		this.workingDir = workingDir;
		this.providerModuleBinds = providerModuleBinds;
		this.configXml = configXml;
		this.atomicAll = atomicAll;

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
		// 开始发布准备阶段。
		var distributeId = 817123; // 使用魔数。发布流程不并发，不需要动态多值，当然提供动态多值更加完善。
		for (var hotAgent : hotAgents) {
			hotAgent.prepareDistribute(distributeId); // 开始远程发布需要禁止服务器自带的定时发布功能。
		}
		// 拷贝文件
		for (var hotAgent : hotAgents) {
			hotAgent.distribute(new File(workingDir, "modules"));
			hotAgent.distribute(new File(workingDir, "interfaces"));
		}
		// 开始发布，参数决定是否两阶段（全局原子化）。
		var futures = new ArrayList<TryDistribute>();
		for (var hotAgent : hotAgents) {
			futures.add(hotAgent.tryDistribute(distributeId, atomicAll));
		}
		// 检查结果
		if (atomicAll) {
			// 全局原子化，检查结果，并回滚或者提交
			try {
				if (checkDistributeFutures(hotAgents, futures)) {
					// 全部成功，提交发布。
					var commit2Futures = new ArrayList<Commit>();
					for (var hotAgent : hotAgents) {
						commit2Futures.add(hotAgent.commit(distributeId));
					}
					// 二次提交，因为发布有个不可出错阶段（不可回滚），所以需要commit两次。
					var hotAgentCommit2 = new ArrayList<HotAgent>();
					for (var i = 0; i < commit2Futures.size(); ++i) {
						try {
							var r = commit2Futures.get(i);
							assert r.getFuture() != null;
							r.getFuture().await(30_000);
							if (r.getResultCode() == 0) {
								hotAgentCommit2.add(hotAgents.get(i)); // 只有成功的才真正commit2。
							} else {
								System.out.println("commit2Future =" + IModule.getErrorCode(r.getResultCode()));
							}
						} catch (Exception ex0) {
							System.out.println("commit2Future exception: " + ex0);
						}
					}
					for (var hotAgent : hotAgentCommit2) {
						try {
							hotAgent.commit2(distributeId);
						} catch (Exception ex0) {
							System.out.println("commit2 exception: " + ex0);
						}
					}
					return; // success done
				}
			} catch (Exception ex) {
				System.out.println("exception: " + ex);
			}
			// rollback
			for (var hotAgent : hotAgents) {
				try {
					hotAgent.tryRollback(distributeId);
				} catch (Exception ex0) {
					System.out.println(hotAgent.getPeer() + "exception: " + ex0);
				}
			}
		} else {
			// 每个独立发布模式，仅检查并报告错误。
			for (var i = 0; i < hotAgents.size(); ++i) {
				try {
					var r = futures.get(i);
					assert r.getFuture() != null;
					r.getFuture().await(30_000);
					if (r.getResultCode() != 0) {
						System.out.println(hotAgents.get(i).getPeer() + "=" + IModule.getErrorCode(r.getResultCode()));
					}
				} catch (Exception ex) {
					System.out.println("exception: " + ex);
				}
			}
		}
	}

	private static boolean checkDistributeFutures(ArrayList<HotAgent> hotAgents,
												  ArrayList<TryDistribute> tryDistributes)
			throws ExecutionException, InterruptedException, TimeoutException {

		for (var i = 0; i < hotAgents.size(); ++i) {
			var rpc = tryDistributes.get(i);
			assert rpc.getFuture() != null;
			rpc.getFuture().await(30_000);
			if (rpc.getResultCode() != 0) {
				System.out.println(hotAgents.get(i).getPeer() + "=" + IModule.getErrorCode(rpc.getResultCode()));
				return false;
			}
		}
		return true;
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

	private static final ArrayList<String> hotManagers = new ArrayList<>();

	public static void main(String [] args) throws Exception {
		var classesDir = "build/classes/java/main";
		var exportBean = true;
		var workingDir = "hot";
		var app = "";
		var providerModuleBinds = "";
		var configXml = "";
		var atomicAll = false;

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
			case "-atomicAll":
				atomicAll = true;
				break;
			default:
				hotManagers.add(args[i]);
				break;
			}
		}

		var distribute = new Distribute(
				classesDir, exportBean, workingDir,
				providerModuleBinds, configXml, atomicAll);

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
