package Zeze.Hot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Service;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 装载所有的模块接口。
 * <p>
 * 0. 参数指定工作目录和更新来源目录，
 * 1. 监视更新来源目录，自动安装升级。
 * 2. 一般全局一个实例。
 * 3. jar覆盖的时候，能装载里面新加入的class，但是同名的已经loadClass的类不会改变。
 */
public class HotManager extends ClassLoader {
	private static final Logger logger = LogManager.getLogger(HotManager.class);

	private final String workingDir;
	private final String distributeDir;
	private final FewModifyMap<File, JarFile> jars = new FewModifyMap<>();

	public static class JarEntry {
		public final JarFile jar;
		public final ZipEntry entry;

		public JarEntry(JarFile jar, ZipEntry entry) {
			this.jar = jar;
			this.entry = entry;
		}
	}

	private final FewModifyMap<String, JarEntry> zipEntries = new FewModifyMap<>();

	// module namespace -> HotModule
	private final FewModifySortedMap<String, HotModule> modules = new FewModifySortedMap<>();
	private final ReentrantReadWriteLock hotLock = new ReentrantReadWriteLock();
	private final Application zeze;
	private final HotRedirect hotRedirect;

	private final ConcurrentHashSet<HotUpgrade> hotUpgrades = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<HotBeanFactory> hotBeanFactories = new ConcurrentHashSet<>();

	private final DistributeManager distributeManager = new DistributeManager(this);
	private final HotDistribute hotDistribute = new HotDistribute(distributeManager);
	private final HotManagerService hotManagerService;

	public DistributeManager getDistributeManager() {
		return distributeManager;
	}

	public HotDistribute getHotDistribute() {
		return hotDistribute;
	}

	public static class HotManagerService extends Service {
		public HotManagerService(Config config) {
			super("Zeze.HotManagerService", config);
		}
	}

	public void addHotBeanFactory(HotBeanFactory hotBeanFactory) {
		hotBeanFactories.add(hotBeanFactory);
	}

	public void removeHotBeanFactory(HotBeanFactory hotBeanFactory) {
		hotBeanFactories.remove(hotBeanFactory);
	}

	public void addHotUpgrade(HotUpgrade hotUpgrade) {
		hotUpgrades.add(hotUpgrade);
	}

	public void removeHotUpgrade(HotUpgrade hotUpgrade) {
		hotUpgrades.remove(hotUpgrade);
	}

	public static boolean isHotModule(ClassLoader cl) {
		return cl.getClass() == HotModule.class;
	}

	public HotRedirect getHotRedirect() {
		return hotRedirect;
	}

	public void destroyModules() {
		modules.clear();
	}

	public void initialize(Map<String, IModule> modulesOut) throws Exception {
		GenModule.instance.getCompiler().useOptions("-cp", buildCp());
		GenModule.instance.getCompiler().useParentClassLoader(getHotRedirect());
		start();

		for (var module : modules.values()) {
			var iModule = (IModule)module.getService();
			iModule.Initialize(zeze.getAppBase());
			modulesOut.put(module.getName(), iModule);
		}
	}

	public HotGuard enterReadLock() {
		return new HotGuard(hotLock.readLock());
	}

	public HotGuard enterWriteLock() {
		return new HotGuard(hotLock.writeLock());
	}

	public HotModule findHotModule(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : modules.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null;
	}

	public <T extends HotService> HotModuleContext<T> getModuleContext(String moduleNamespace, Class<T> serviceClass) {
		var module = modules.get(moduleNamespace);
		if (null == module)
			return null; // 允许外面主动判断，用于动态判断服务。
		return module.getContext(serviceClass);
	}

	public String buildCp() {
		var sb = new StringBuilder();
		for (var jar : jars.keySet()) {
			sb.append(jar).append(File.pathSeparatorChar);
		}
		for (var module : modules.values()) {
			sb.append(module.getJarFileName()).append(File.pathSeparatorChar);
		}
		for (var path : Reflect.collectClassPaths(ClassLoader.getSystemClassLoader()))
			sb.append(path).append(File.pathSeparatorChar);

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private IModule[] createModuleInstance(Collection<HotModule> result) throws Exception {
		var moduleClasses = new Class[result.size()];
		var i = 0;
		for (var module : result)
			moduleClasses[i++] = module.getModuleClass();
		IModule[] iModules = GenModule.instance.createRedirectModules(zeze.getAppBase(), moduleClasses);
		if (null == iModules) {
			// 这种情况是不是内部处理掉比较好。
			// redirect return null, try new without redirect.
			iModules = new IModule[moduleClasses.length];
			for (var ii = 0; ii < moduleClasses.length; ++ii) {
				iModules[ii] = (IModule)moduleClasses[ii]
						.getConstructor(zeze.getAppBase().getClass()).newInstance(zeze.getAppBase());
			}
		}
		return iModules;
	}

	private static Bean retreat(ArrayList<HotModule> removes, ArrayList<HotModule> currents, Bean bean) {
		// 判断是否当前正在热更的模块创建的。
		try {
			var cl = bean.getClass().getClassLoader();
			if (HotManager.isHotModule(cl)) {
				var indexHot = removes.indexOf((HotModule)cl);
				// removes 里面可能存在null。
				// indexOf找得到，currents里面就肯定有。但是安装可能部分失败。所以还是需要判断indexHot。
				if (indexHot >= 0 && indexHot < currents.size()) {
					var current = currents.get(indexHot);
					if (null != current) {
						var curClass = current.loadClass(bean.getClass().getName());
						var curBean = (Bean)curClass.getConstructor().newInstance();
						var bb = ByteBuffer.Allocate();
						bean.encode(bb);
						curBean.decode(bb);
//					logger.info("<------ retreat ------> {} \r\n{} \r\n{}",
//							bean.getClass().getName(), bean.variables(), curBean.variables());
						return curBean;
					}
				}
			}
		} catch (Throwable ex) {
			logger.error("retreat", ex);
		}
		return null;
	}

	/**
	 * Schemas Jar 命名规则：__hot_schemas__{SolutionName}.jar
	 * 这个函数找到jar并且解析出SolutionName，然后Class.forName("{SolutionName}.Schemas", ...)
	 * 每次Class.forName都是用新的ClassLoader，这里借用HotModule来实现装载。
	 */
	public static final String SchemasPrefix = "__hot_schemas__";
	public static final String SchemasSuffix = ".jar";

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Schemas loadSchemas() throws Exception {
		File schemasJarFile = null;
		String solutionName = null;

		var listFiles = new File(distributeDir).listFiles();
		if (null == listFiles)
			throw new RuntimeException("no files.");

		for (var file : listFiles) {
			var fileName = file.getName();
			if (file.isDirectory() || !fileName.startsWith(SchemasPrefix) || !fileName.endsWith(SchemasSuffix))
				continue;
			schemasJarFile = file;
			solutionName = fileName.substring(SchemasPrefix.length(), fileName.length() - SchemasSuffix.length());
			break;
		}
		if (null == schemasJarFile)
			throw new RuntimeException("__hot_schemas__{SolutionName}.jar not found.");

		try (var schemasCl = new HotModule(schemasJarFile)) {
			var schemasClass = Class.forName(solutionName + ".Schemas", true, schemasCl);
			return (Schemas)schemasClass.getConstructor().newInstance();
		} finally {
			schemasJarFile.delete();
		}
	}

	private volatile boolean upgrading = false;

	public boolean isUpgrading() {
		return upgrading;
	}

	private void recoverModules(ArrayList<HotModule> exists, int reverseErrorIndex) {
		// 1. 停止是从exists后面反过来遍历的。
		// 2. reverseErrorIndex 不恢复。
		// 3. 重启过程错误的不恢复。

		// 已经停止的，重新启动，并且加入modules。
		for (var i = reverseErrorIndex + 1; i < exists.size(); ++i) {
			var exist = exists.get(i);
			if (null == exist)
				continue;
			try {
				((IModule)exist.getService()).Register(); // 已经注销过，重启需要再次注册。
				exist.start();
				modules.put(exist.getName(), exist);
			} catch (Throwable ex) {
				// 恢复过程中重新启动失败，简单忽略。
				logger.error("recover {}", exist, ex);
			}
		}
		// 还没有停止的，重新加入modules。
		for (var i = reverseErrorIndex - 1; i >= 0; --i) {
			var exist = exists.get(i);
			if (null == exist)
				continue;
			modules.put(exist.getName(), exist);
		}
		if (!getReadyLines().isEmpty() && getReadyLines().get(0).startsWith("stop")) {
			// 停止服务时，即使当前停止错误的模块也恢复。【调试代码】
			if (reverseErrorIndex >= 0) {
				var exist = exists.get(reverseErrorIndex);
				try {
					((IModule)exist.getService()).Register(); // 已经注销过，重启需要再次注册。
					exist.start();
					modules.put(exist.getName(), exist);
				} catch (Throwable ex) {
					// 恢复过程中重新启动失败，简单忽略。
					logger.error("recover {}", exist, ex);
				}
			}
		} else if (reverseErrorIndex >= 0) {
			// 发生错误的禁止暴露服务。
			exists.get(reverseErrorIndex).disable();
		}
	}

	public class MainRollbackAction implements Action0 {
		private final Zeze.Application zeze;
		private final ArrayList<HotModule> exists;
		private final Schemas oldSchemas;
		private boolean hasAlter = false;

		public MainRollbackAction(Zeze.Application zeze, ArrayList<HotModule> exists, Schemas oldSchemas) {
			this.zeze = zeze;
			this.exists = exists;
			this.oldSchemas = oldSchemas;
		}

		public void setHasAlter() {
			hasAlter = true;
		}

		@Override
		public void run() {
			// 恢复schemas
			zeze.__upgrade_schemas__(oldSchemas);
			// 导致HotModule重新start并导致Register。
			recoverModules(exists, -1);
			// 在恢复的Schemas的旧结构下，重新alter。
			if (hasAlter)
				zeze.__install_alter__();
			// 内存表的回滚保持旧数据即可，在TableX.open(exist, app)里面处理。
			zeze.__get_upgrade_memory_table__().clear();
			logger.info("________________ install rollback ________________");
		}
	}

	private ArrayList<HotModule> install(List<String> namespaces, boolean atomicAll) throws Exception {
		logger.info("________________ install ________________ {}", namespaces);
		try {
			upgrading = true;
			// 首先查询得到现有的模块，不存在也加入null。
			var exists = new ArrayList<HotModule>(namespaces.size());
			for (var namespace : namespaces)
				exists.add(modules.get(namespace));
			// 锁外执行stopBefore
			for (var exist : exists) {
				if (null != exist)
					exist.stopBefore();
			}
			var result = new ArrayList<HotModule>();
			try (var ignored = enterWriteLock()) {
				var app = zeze.getAppBase();
				// 先保存现有的数据
				app.getZeze().checkpointRun();
				// remove
				for (var namespace : namespaces) {
					modules.remove(namespace);
				}
				// reverse stop
				var reverseIndex = exists.size() - 1;
				try {
					for (; reverseIndex >= 0; --reverseIndex) {
						var exist = exists.get(reverseIndex);
						if (exist != null)
							exist.stop();
					}
				} catch (Throwable ex) {
					logger.error("stop modules {}", exists, ex);
					recoverModules(exists, reverseIndex); // 此时的恢复，schemas没有变化，不需要重新alter。
					return null;
				}

				var txn = new HotTransaction("HotManager.install");
				app.getZeze().__install_prepare__();
				var old = app.getZeze().__upgrade_schemas__(loadSchemas());

				var mainRollbackAction = new MainRollbackAction(zeze, exists, old);
				txn.whileRollback(mainRollbackAction);

				ArrayList<JarFile> hotJarFiles;
				ArrayList<HotModule> newModules;
				try {
					// install
					for (var namespace : namespaces) {
						result.add(_install(namespace, txn));
					}
					// batch load redirect
					var iModules = createModuleInstance(result);
					txn.whileRollback(() -> {
						for (var iModule : iModules)
							iModule.UnRegister();
					});
					app.getZeze().__install_alter__();
					mainRollbackAction.setHasAlter();

					hotJarFiles = new ArrayList<>();
					newModules = new ArrayList<>();
					for (var ii = 0; ii < iModules.length; ++ii) {
						var exist = exists.get(ii);
						var module = result.get(ii);
						hotJarFiles.add(module.getJarFile());
						module.setService(iModules[ii]);
						if (null != exist) {
							module.upgrade(exist); // 事务外运行，对于继承升级应该都是事务外的。
						} else {
							newModules.add(module);
						}
					}

					if (atomicAll)
						hotDistribute.sendTryDistributeResultAndWaitCommit(0);

					txn.commit();
				} catch (Throwable ex) {
					logger.error("", ex);
					txn.rollback();
					return null;
				}
				// 下面进入”不能“出错阶段：1. start 出错则相关模块停止服务；2. 其他错误，停止程序。
				try {
					// 内部停止，不可恢复
					for (var exist : exists) {
						if (null != exist)
							exist.stopInternal();
					}
					// 获取优化后的内部关联接口
					var freshHotUpgrades = new ArrayList<HotUpgrade>();
					for (var hotUpgrade : hotUpgrades) {
						if (hotUpgrade.hasFreshStopModuleLocalOnce()) {
							freshHotUpgrades.add(hotUpgrade);
						}
					}
					var freshHotBeanFactories = new ArrayList<HotBeanFactory>();
					for (var hotBeanFactory : hotBeanFactories) {
						if (hotBeanFactory.hasFreshStopModuleDynamicOnce()) {
							freshHotBeanFactories.add(hotBeanFactory);
						}
					}
					// internal upgrade
					for (var hotUpgrade : freshHotUpgrades) {
						hotUpgrade.upgrade((bean) -> retreat(exists, result, bean));
					}
					// upgrade user memory table
					for (var table : app.getZeze().__get_upgrade_memory_table__()) {
						table.upgrade();
					}
					app.getZeze().__get_upgrade_memory_table__().clear();
					var beanFactories = new HashMap<BeanFactory, List<Class<?>>>();
					for (var hotBeanFactory : freshHotBeanFactories) {
						hotBeanFactory.clearTableCache();
						beanFactories.put(hotBeanFactory.beanFactory(), new ArrayList<>());
					}
					BeanFactory.resetHot(beanFactories, hotJarFiles);
					for (var hotBeanFactory : freshHotBeanFactories) {
						hotBeanFactory.processWithNewClasses(beanFactories.get(hotBeanFactory.beanFactory()));
					}
					// start ordered
					var startErrors = new ArrayList<HotModule>();
					for (var module : result) {
						try {
							module.start();
						} catch (Exception ex) {
							logger.error("", ex);
							startErrors.add(module);
						}
					}
					// 再次停止启动失败的模块，并注销。
					for (var startErrorIndex = startErrors.size() - 1; startErrorIndex >= 0; --startErrorIndex) {
						var module = modules.remove(startErrors.get(startErrorIndex).getName());
						module.stop();
						//module.disable(); // stop() has call disable()
					}
					for (var module : newModules) {
						if (startErrors.contains(module))
							continue;
						var moduleConfig = module.loadModuleConfig();
						zeze.getProviderApp().providerService.addHotModule((IModule)module.getService(), moduleConfig);
					}

					// 本来这个代码应该放在下面的final commit point. 那里。
					// 但有个超时错误需要处理，这个错误也停止进程。
					// 由于后续的startLast是忽略错误的，所以代码移到这里也是可以的。
					if (atomicAll) {
						hotDistribute.sendCommitResultAndWaitCommit2(0);
					}
				} catch (Throwable ex) {
					logger.error("too much changes. impossible to rollback.", ex);
					LogManager.shutdown();
					Runtime.getRuntime().halt(111222);
				}
			}
			// 最后启动，startLast.
			for (var module : result) {
				try {
					module.startLast();
				} catch (Exception ex) {
					logger.error("", ex);
				}
			}
			// final commit point.
			return result;
		} finally {
			upgrading = false;
		}
	}

	private void putJar(File file) throws IOException {
		var jar = new JarFile(file);
		// replace all entry
		// 新的jar的entry会完全覆盖旧的，所以不用考虑删除。
		for (var e = jar.entries(); e.hasMoreElements(); ) {
			var entry = e.nextElement();
			var eName = entry.getName().replace('\\', '/').replace('/', '.');
			var javaClassName = eName.substring(0, eName.length() - ".class".length());
			zipEntries.put(javaClassName, new JarEntry(jar, entry));
		}
		jars.put(file, jar);
	}

	/**
	 * @param namespace module name
	 * @return HotModule
	 * @throws Exception error
	 */
	private HotModule _install(String namespace, HotTransaction txn) throws Exception {
		var moduleSrc = Path.of(distributeDir, namespace + ".jar").toFile();
		var interfaceSrc = Path.of(distributeDir, namespace + ".interface.jar").toFile();
		if (!moduleSrc.exists() || !interfaceSrc.exists())
			throw new RuntimeException("distributes not ready.");

		// 安装 interface
		var interfaceDst = Path.of(workingDir, "interfaces", namespace + ".interface.jar").toFile().getCanonicalFile();
		{
			var oldI = jars.remove(interfaceDst);
			if (null != oldI)
				oldI.close();
		}
		var interfaceDstBackup = Path.of(workingDir, "interfaces", namespace + ".interface.jar.backup").toFile();
		if (!interfaceDst.renameTo(interfaceDstBackup))
			throw new RuntimeException("backup interface.jar fail. " + interfaceDst + " -> " + interfaceDstBackup);
		// 从备份恢复，并且重新加载旧文件。
		txn.whileRollback(() -> {
			if (interfaceDstBackup.renameTo(interfaceDst))
				putJar(interfaceDst);
			else
				logger.error("restore interface backup fail {} -> {}", interfaceDstBackup, interfaceDst);
		});
		// 提交的时候删除备份
		txn.whileCommit(() -> Files.deleteIfExists(interfaceDstBackup.toPath()));
		throwIfMatch("install1");

		if (!interfaceSrc.renameTo(interfaceDst))
			throw new RuntimeException("rename fail. " + interfaceSrc + "->" + interfaceDst);
		putJar(interfaceDst);
		txn.whileRollback(() -> {
			if (!interfaceDst.renameTo(interfaceSrc))
				logger.error("uninstall {} -> {} fail", interfaceDst, interfaceSrc);
		});
		throwIfMatch("install2");

		// 安装 module
		var moduleDst = Path.of(workingDir, "modules", namespace + ".jar");
		var moduleDstBackup = Path.of(workingDir, "modules", namespace + ".jar.backup").toFile();
		if (!moduleDst.toFile().renameTo(moduleDstBackup))
			throw new RuntimeException("backup module.jar fail. " + moduleDst + " -> " + moduleDstBackup);

		txn.whileRollback(() -> {
			if (!moduleDstBackup.renameTo(moduleDst.toFile()))
				logger.error("restore module backup fail {} -> {}", moduleDstBackup, moduleDst);
			// module.jar 不用预先装载，在旧HotModule重启的时候会用本来的文件名重新打开。
		});
		txn.whileCommit(() -> Files.deleteIfExists(moduleDstBackup.toPath()));
		throwIfMatch("install3");

		var moduleDstFile = moduleDst.toFile();
		if (!moduleSrc.renameTo(moduleDstFile))
			throw new RuntimeException("rename fail. " + moduleSrc + "->" + moduleDst);

		txn.whileRollback(() -> {
			if (!moduleDstFile.renameTo(moduleSrc))
				logger.error("uninstall {} -> {} fail", moduleDstFile, moduleSrc);
		});
		throwIfMatch("install4");

		var module = new HotModule(this, namespace, moduleDstFile);
		modules.put(module.getName(), module);
		return module;
	}

	public Application getZeze() {
		return zeze;
	}

	public HotManager(AppBase app, String workingDir, String distributeDir) throws Exception {
		hotManagerService = new HotManagerService(app.getZeze().getConfig());

		//System.out.println(workingDir);
		//System.out.println(distributeDir);
		BeanFactory.setApplication(app.getZeze());

		var distributePath = Path.of(distributeDir);
		var interfacesPath = Path.of(workingDir, "interfaces");
		if (distributePath.startsWith(interfacesPath))
			throw new RuntimeException("distributeDir is sub-dir of workingDir/interfaces/");

		var modulePath = Path.of(workingDir, "modules");
		if (distributePath.startsWith(modulePath))
			throw new RuntimeException("distributeDir is sub-dir of workingDir/modulebus/");

		if (Path.of(workingDir).startsWith(distributePath))
			throw new RuntimeException("workingDir is sub-dir of distributeDir");

		if (!Files.isDirectory(distributePath) && GenModule.instance.genFileSrcRoot == null) {
			throw new FileNotFoundException(
					"distributePath = " + distributePath
							+ ", curPath = " + new File("."));
		}

		this.workingDir = workingDir;
		this.distributeDir = distributeDir;
		this.zeze = app.getZeze();
		this.hotRedirect = new HotRedirect(this);

		this.loadExistInterfaces(interfacesPath.toFile());
		this.loadExistModules(modulePath.toFile());

		hotDistribute.RegisterProtocols(hotManagerService);
	}

	/**
	 * 使用即时命令，可以更快速的得到响应。
	 * 准备把 Distribute.java 发展成发布工具。
	 * 支持远程，多服务器发布。
	 * 支持原子发布。
	 */
	public long tryDistribute(boolean atomicAll) {
		var rc = 0L;
		try {
			var ready = Path.of(distributeDir, "ready");
			if (Files.exists(ready)) {
				try {
					readyLines = Files.readAllLines(ready);
					if (null != installReadies(atomicAll))
						Files.deleteIfExists(ready); // success
					else {
						rc = IModule.errorCode(HotDistribute.ModuleId, HotDistribute.eInstall);
						// 安装失败，使用这个错误码报告最终错误。
						// 可能不需要报告，里面的流程已经报告完成。
						renameDistributes();
					}
					return 0;
				} catch (Throwable ex) {
					logger.error("", ex);
					renameDistributes();
				}
			}
		} catch (Throwable ex) {
			logger.error("distributeDir = '{}'", distributeDir, ex);
			rc = Procedure.Exception;
		} finally {
			hotDistribute.setIdle(rc);
		}
		return Procedure.Exception;
	}

	public void start() throws Exception {
		var iModules = createModuleInstance(modules.values());
		var i = 0;

		// 这里要求modules.values()遍历顺序稳定，在modules没有改变时，应该是符合要求的吧。
		for (var module : modules.values()) {
			module.setService(iModules[i++]);
		}

		Task.getScheduledThreadPool().scheduleAtFixedRate(
				() -> tryDistribute(false), 10000, 10000, TimeUnit.MILLISECONDS);
		Task.hotGuard = this::enterReadLock;
		hotManagerService.start();
	}

	private volatile java.util.List<String> readyLines;

	// 用来控制错误模拟，目前是调试用的。
	public java.util.List<String> getReadyLines() {
		return readyLines;
	}

	public void throwIfMatch(String step) {
		if (null != getReadyLines() && !getReadyLines().isEmpty() && getReadyLines().get(0).startsWith(step))
			throw new RuntimeException("throwExceptionIfMatch " + step + ", " + getReadyLines().get(0));
	}

	public void renameDistributes() throws IOException {
		var files = new File(distributeDir).listFiles();
		var formatter = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss-SSS");
		var backupDir = Path.of(distributeDir, "backup", formatter.format(new Date()));
		//noinspection ResultOfMethodCallIgnored
		backupDir.toFile().mkdirs();
		var backupDirFile = backupDir.toFile();
		if (null != files) {
			for (var file : files) {
				if (file.isDirectory())
					continue;
				if (!file.renameTo(new File(backupDirFile, file.getName())))
					logger.error("rename {} fail", file);
			}
		}
	}

	public void startModule(String moduleName) throws Exception {
		var module = modules.get(moduleName);
		if (null != module)
			module.start();
	}

	public void startModulesExcept(Set<String> except) throws Exception {
		for (var module : modules.values()) {
			if (except.contains(module.getName()))
				continue;
			module.start();
		}
	}

	public void startLastModule(String moduleName) throws Exception {
		var module = modules.get(moduleName);
		if (null != module)
			module.startLast();
	}

	public void startLastModulesExcept(Set<String> except) throws Exception {
		for (var module : modules.values()) {
			if (except.contains(module.getName()))
				continue;
			module.startLast();
		}
	}

	public void stopModule(String moduleName) throws Exception {
		hotManagerService.stop();
		var module = modules.get(moduleName);
		if (null != module)
			module.stop();
	}

	public void stopModulesExcept(Set<String> except) throws Exception {
		for (var module : modules.values()) {
			if (except.contains(module.getName()))
				continue;
			module.stop();
		}
	}

	public void stopBeforeModule(String moduleName) throws Exception {
		var module = modules.get(moduleName);
		if (null != module)
			module.stopBefore();
	}

	public void stopBeforeModulesExcept(Set<String> except) throws Exception {
		for (var module : modules.values()) {
			if (except.contains(module.getName()))
				continue;
			module.stopBefore();
		}
	}

	private void loadExistModules(File dir) throws Exception {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			var filename = file.getName();
			if (filename.endsWith(".jar")) {
				var namespace = filename.substring(0, filename.indexOf(".jar")); // Temp.jar
				var module = new HotModule(this, namespace, file);
				modules.put(namespace, module);
			}

			if (file.isDirectory())
				loadExistModules(file);
		}
	}

	private void loadExistInterfaces(File dir) throws IOException {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.getName().endsWith(".jar"))
				putJar(file.getCanonicalFile());

			if (file.isDirectory())
				loadExistInterfaces(file);
		}
	}

	public String getWorkingDir() {
		return workingDir;
	}

	public String getDistributeDir() {
		return distributeDir;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		var e = zipEntries.get(className);
		if (null != e) {
			try (var inputStream = e.jar.getInputStream(e.entry)) {
				var bytes = inputStream.readAllBytes();
				return defineClass(className, bytes, 0, bytes.length);
			} catch (IOException ex) {
				Task.forceThrow(ex);
			}
		}
		return super.findClass(className);
	}

	public ArrayList<HotModule> installReadies(boolean atomicAll) throws Exception {
		var foundJars = new HashSet<String>();
		var readies = new HashSet<String>();
		loadExistDistributes(foundJars, readies);

		var readiesOrder = new ArrayList<String>();
		var fOrder = new File(distributeDir, "start.order.txt");
		if (fOrder.exists()) {
			var lines = Files.readAllLines(fOrder.toPath());
			for (var line : lines) {
				if (readies.remove(line))
					readiesOrder.add(line);
			}
			//noinspection ResultOfMethodCallIgnored
			fOrder.delete();
		}
		// add remain
		readiesOrder.addAll(readies);
		return install(readiesOrder, atomicAll);
	}

	private static void tryReady(HashSet<String> foundJars, String jarFileName, HashSet<String> readies) {
		try {
			final var fileName = jarFileName.substring(0, jarFileName.indexOf(".jar"));
			//System.out.println("tryInstall " + fileName);

			if (fileName.endsWith(".interface")) {
				var namespace = fileName.substring(0, fileName.indexOf(".interface"));
				if (foundJars.remove(namespace)) {
					readies.add(namespace);
					return; // done
				}
			} else {
				var interfaceJar = fileName + ".interface";
				if (foundJars.remove(interfaceJar)) {
					readies.add(fileName);
					return; // done
				}
			}
			// 两个jar包只发现了一个，先存下来。
			foundJars.add(fileName);
		} catch (Exception ex) {
			logger.error("", ex);
		}
	}

	private void loadExistDistributes(HashSet<String> foundJars, HashSet<String> readies) {
		var files = new File(distributeDir).listFiles();
		if (null == files) {
			System.out.println("is null.");
			return;
		}

		for (var file : files) {
			//System.out.println(file + " " + file.isDirectory());
			if (file.isDirectory())
				continue; // 不支持子目录。

			if (file.getName().endsWith(".jar") && !file.getName().startsWith(SchemasPrefix))
				tryReady(foundJars, file.getName(), readies);
		}
	}
}
