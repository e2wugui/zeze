package Zeze.Hot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.IModule;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * 装载所有的模块接口。
 *
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

	// module namespace -> HotModule
	private final FewModifySortedMap<String, HotModule> modules = new FewModifySortedMap<>();
	private volatile boolean running = true;
	private volatile Thread worker;

	private final ReentrantReadWriteLock hotLock = new ReentrantReadWriteLock();
	private final AppBase app;

	public void destroyModules() {
		modules.clear();
	}

	public void initialize(Map<String, IModule> modulesOut) throws Exception {
		for (var module : modules.values()) {
			var iModule = (IModule)module.getService();
			iModule.Initialize(app);
			modulesOut.put(module.getName(), iModule);
		}
	}

	public HotGuard enterReadLock() {
		return new HotGuard(hotLock.readLock());
	}

	public HotGuard enterWriteLock() {
		return new HotGuard(hotLock.writeLock());
	}

	/*
	// 采用其他管理措施以后，这个方法很可能不需要了。
	private HotModule find(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : modules.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null; // throw ?
	}
	*/

	public <T extends HotService> HotModuleContext<T> getModuleContext(String moduleNamespace, Class<T> serviceClass) {
		var module = modules.get(moduleNamespace);
		if (null == module)
			return null; // 允许外面主动判断，用于动态判断服务。
		return module.getContext(serviceClass);
	}

	@SuppressWarnings("unchecked")
	private IModule[] createModuleInstance(Collection<HotModule> result) throws Exception {
		var moduleClasses = new Class[result.size()];
		var i = 0;
		for (var module : result)
			moduleClasses[i++] = module.getModuleClass();
		IModule[] iModules = GenModule.instance.createRedirectModules(app, moduleClasses);
		if (null == iModules) {
			// todo @张路 这种情况是不是内部处理掉比较好。
			// redirect return null, try new without redirect.
			iModules = new IModule[moduleClasses.length];
			for (var ii = 0; ii < moduleClasses.length; ++ii) {
				iModules[ii] = (IModule)moduleClasses[ii].getConstructor(app.getClass()).newInstance(app);
			}
		}
		return iModules;
	}

	public List<HotModule> install(List<String> namespaces) throws Exception {
		try (var ignored = enterWriteLock()) {
			var result = new ArrayList<HotModule>(namespaces.size());
			var exists = new ArrayList<HotModule>(namespaces.size());

			// remove
			for (var namespace : namespaces) {
				exists.add(modules.remove(namespace));
			}
			// reverse stop
			for (var reverseI = exists.size() - 1; reverseI >= 0; --reverseI) {
				var exist = exists.get(reverseI);
				if (exist != null)
					exist.stop();
			}
			// install
			for (var namespace : namespaces)
				result.add(_install(namespace));
			// batch load redirect
			var iModules = createModuleInstance(result);
			for (var ii = 0; ii < iModules.length; ++ii) {
				var exist = exists.get(ii);
				var module = result.get(ii);
				module.setService(iModules[ii]);
				if (null != exist)
					module.upgrade(exist);
			}
			// start ordered
			for (var module : result)
				module.start();
			return result;
		}
	}

	public HotModule install(String namespace) throws Exception {
		try (var ignored = enterWriteLock()) {
			var exist = modules.remove(namespace);
			if (null != exist)
				exist.stop();
			var module = _install(namespace);
			var iModules = createModuleInstance(List.of(module));
			// redirect return null, try new without redirect.
			module.setService(iModules == null
					? (IModule)module.getModuleClass().getConstructor(app.getClass()).newInstance(app)
					: iModules[0]);
			if (exist != null)
				module.upgrade(exist);
			module.start();
			return module;
		}
	}

	/**
	 * todo【警告】安装过程目前没有事务性，如果安装出现错误，可能导致某些模块出错。
	 *
	 * @param namespace module name
	 * @return HotModule
	 * @throws Exception error
	 */
	private HotModule _install(String namespace) throws Exception {
		var moduleSrc = Path.of(distributeDir, namespace + ".jar").toFile();
		var interfaceSrc = Path.of(distributeDir, namespace + ".interface.jar").toFile();
		if (!moduleSrc.exists() || !interfaceSrc.exists())
			throw new RuntimeException("distributes not ready.");

		// todo 生命期管理，确定服务是否可用，等等。
		// 安装 interface
		var interfaceDstAbsolute = Path.of(workingDir, "interfaces", namespace + ".interface.jar")
				.toFile().getAbsoluteFile();
		{
			var oldI = jars.remove(interfaceDstAbsolute);
			if (null != oldI)
				oldI.close();
		}
		for (var i = 0; i < 10; ++i) {
			try {
				if (Files.deleteIfExists(interfaceDstAbsolute.toPath()))
					break;
			} catch (Exception ex) {
				logger.error("", ex);
			}
			System.out.println("delete interface fail=" + i + ":" + interfaceDstAbsolute);
			Thread.sleep(100);
		}
		if (!interfaceSrc.renameTo(interfaceDstAbsolute))
			throw new RuntimeException("rename fail. " + interfaceSrc + "->" + interfaceDstAbsolute);
		jars.put(interfaceDstAbsolute, new JarFile(interfaceDstAbsolute));

		// 安装 module
		var moduleDst = Path.of(workingDir, "modules", namespace + ".jar");
		for (var i = 0; i < 10; ++i) {
			try {
				if (Files.deleteIfExists(moduleDst))
					break;
			} catch (Exception ex) {
				logger.error("", ex);
			}
			System.out.println("delete module fail=" + i + ":" + moduleDst);
			Thread.sleep(100);
		}
		var moduleDstFile = moduleDst.toFile();
		if (!moduleSrc.renameTo(moduleDstFile))
			throw new RuntimeException("rename fail. " + moduleSrc + "->" + moduleDst);
		var module = new HotModule(this, namespace, moduleDstFile);
		modules.put(module.getName(), module);
		return module;
	}

	public HotManager(AppBase app, String workingDir, String distributeDir) throws Exception {
		//System.out.println(workingDir);
		//System.out.println(distributeDir);

		var distributePath = Path.of(distributeDir);
		var interfacesPath = Path.of(workingDir, "interfaces");
		if (distributePath.startsWith(interfacesPath))
			throw new RuntimeException("distributeDir is sub-dir of workingDir/interfaces/");

		var modulePath = Path.of(workingDir, "modules");
		if (distributePath.startsWith(modulePath))
			throw new RuntimeException("distributeDir is sub-dir of workingDir/modules/");

		if (Path.of(workingDir).startsWith(distributePath))
			throw new RuntimeException("workingDir is sub-dir of distributeDir");

		if (!Files.isDirectory(distributePath) && GenModule.instance.genFileSrcRoot == null) {
			throw new FileNotFoundException("distributePath = " + distributePath
					+ ", curPath = " + new File(".").getAbsolutePath());
		}

		this.workingDir = workingDir;
		this.distributeDir = distributeDir;
		this.app = app;

		this.loadExistInterfaces(interfacesPath.toFile());
		this.loadExistModules(modulePath.toFile());
		var iModules = createModuleInstance(modules.values());
		var i = 0;
		// 这里要求modules.values()遍历顺序稳定，在modules没有改变时，应该是符合要求的吧。
		for (var module : modules.values()) {
			module.setService(iModules[i++]);
		}
	}

	public void startModules(List<String> startOrder) throws Exception {
		// 先按定义顺序启动模块。
		for (var start : startOrder) {
			var module = modules.get(start);
			if (module != null)
				module.start();
		}
		// 启动剩余的模块。
		for (var module : modules.values()) {
			// 这个是list，可以做点优化：module里面允许重复调用，但只执行一次。
//			if (startOrder.contains(module.getName()))
//				continue; // 忽略已经启动的。
			module.start();
		}
		startWatch();
	}

	public void stopModules(List<String> stopOrder) throws Exception {
		// 逆序
		// 先停止没定义的。
		for (var module : modules.values()) {
			// startModules 的 list.contains 做了优化，但是 stop 需要先停没定义的，没法优化了。
			// 这里用一个HashSet过重了。一般list元素不会太多的话，这里不会有问题。
			// 另外还有个办法，程序退出的时候就不要执行stop，直接退出。
			// 或者stop也先停定义的？
			if (stopOrder.contains(module.getName()))
				continue;
			module.stop();
		}
		// 按定义顺序停止。
		for (var stop : stopOrder) {
			var module = modules.get(stop);
			if (null != module)
				module.stop();
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
				jars.put(file.getAbsoluteFile(), new JarFile(file));

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
		for (var jar : jars.values()) {
			var loaded = loadInterfaceClass(className, jar);
			if (null != loaded) {
				//System.out.println("HotManger.interface=" + className);
				return loaded;
			}
		}
		return super.findClass(className);
	}

	private Class<?> loadInterfaceClass(String className, JarFile jar) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
		if (null == entry)
			return null;
		// 采用标准方式重载findClass以后，不需要判断这个了。
//		var loaded = findLoadedClass(className);
//		if (null != loaded)
//			return loaded;
		try (var inputStream = jar.getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			return defineClass(className, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void startWatch() throws IOException {
		if (null != worker)
			return;

		worker = new Thread(this::watch);
		worker.setDaemon(true);
		worker.start();
	}

	// 严格的话，最好调用这个停止监视线程。
	public void stopWatchAndJoin() throws InterruptedException {
		running = false;

		var tmp = worker;
		if (null != tmp)
			tmp.join();

		synchronized (this) {
			worker = null;
		}
	}

	private void watch() {
		while (running) {
			try {
				doWatch();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
	}

	public void doWatch() throws Exception {
		var foundJars = new HashSet<String>();

		// 1. 注册订阅文件变更事件。
		var watcher = FileSystems.getDefault().newWatchService();
		var distributePath = Path.of(distributeDir);
		distributePath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

		// 2. 先装载已经存在的jar。
		loadExistDistributes(foundJars);

		// 3. 按这个顺序，新加入的jar不会丢失。但是需要处理可能重复的（目前用Files.exists判断一下，见下面）。

		while (running) {
			var key = watcher.poll(200, TimeUnit.MILLISECONDS);
			if (null == key)
				continue;
			for (var event: key.pollEvents()) {
				var kind = event.kind();

				// This key is registered only for ENTRY_CREATE events,
				// but an OVERFLOW event can occur regardless if events
				// are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}

				if (kind == ENTRY_CREATE) {
					// The filename is the context of the event.
					@SuppressWarnings("unchecked")
					var ev = (WatchEvent<Path>)event;
					var file = ev.context();
					var filename = file.getFileName().toString();
					if (filename.endsWith(".jar") && Files.exists(file))
						tryInstall(foundJars, filename);
				}
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events.  If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			if (!key.reset()) {
				break;
			}
		}
	}

	private void tryInstall(HashSet<String> foundJars, String jarFileName) {
		try {
			final var fileName = jarFileName.substring(0, jarFileName.indexOf(".jar"));
			System.out.println("tryInstall " + fileName);

			if (fileName.endsWith(".interface")) {
				var namespace = fileName.substring(0, fileName.indexOf(".interface"));
				if (foundJars.remove(namespace)) {
					install(namespace);
					return; // done
				}
			} else {
				var interfaceJar = fileName + ".interface";
				if (foundJars.remove(interfaceJar)) {
					install(fileName);
					return; // done
				}
			}
			// 两个jar包只发现了一个，先存下来。
			foundJars.add(fileName);
		} catch (Exception ex) {
			logger.error("", ex);
		}
	}

	private void loadExistDistributes(HashSet<String> foundJars) {
		var files = new File(distributeDir).listFiles();
		if (null == files) {
			System.out.println("is null.");
			return;
		}

		for (var file : files) {
			System.out.println(file + " " + file.isDirectory());
			if (file.isDirectory())
				continue; // 不支持子目录。

			tryInstall(foundJars, file.getName());
		}
	}
}
