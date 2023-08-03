package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import Zeze.AppBase;
import Zeze.IModule;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.List;

/**
 * 装载所有的模块接口。
 *
 * 0. 参数指定工作目录和更新来源目录，
 * 1. 监视更新来源目录，自动安装升级。
 * 2. 一般全局一个实例。
 * 3. jar覆盖的时候，能装载里面新加入的class，但是同名的已经loadClass的类不会改变。
 */
public class HotManager extends ClassLoader {
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

	public <T extends HotService> HotModuleContext<T> getModuleContext(String moduleNamespace, Class<T> serviceClass) {
		var module = modules.get(moduleNamespace);
		if (null == module)
			return null; // 允许外面主动判断，用于动态判断服务。
		return module.getContext(serviceClass);
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
			for (var i = 0; i < namespaces.size(); ++i)
				result.add(_install(namespaces.get(i), exists.get(i)));
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
			var module = _install(namespace, exist);
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
	private HotModule _install(String namespace, HotModule exist) throws Exception {
		var moduleSrc = Path.of(distributeDir, namespace + ".jar").toFile();
		var interfaceSrc = Path.of(distributeDir, namespace + ".interface.jar").toFile();
		if (!moduleSrc.exists() || !interfaceSrc.exists())
			throw new RuntimeException("distributes not ready.");

		// todo 生命期管理，确定服务是否可用，等等。
		// 安装 interface
		var interfaceDstAbsolute = Path.of(workingDir, "interfaces", namespace + ".interface.jar")
				.toFile().getAbsoluteFile();
		var oldI = jars.remove(interfaceDstAbsolute);
		if (null != oldI)
			oldI.close();
		Files.deleteIfExists(interfaceDstAbsolute.toPath());
		if (!interfaceSrc.renameTo(interfaceDstAbsolute))
			throw new RuntimeException("rename fail. " + interfaceSrc + "->" + interfaceDstAbsolute);
		jars.put(interfaceDstAbsolute, new JarFile(interfaceDstAbsolute));

		// 安装 module
		var moduleDst = Path.of(workingDir, "modules", namespace + ".jar").toFile();
		Files.deleteIfExists(moduleDst.toPath());
		if (!moduleSrc.renameTo(moduleDst))
			throw new RuntimeException("rename fail. " + moduleSrc + "->" + moduleDst);
		var module = new HotModule(app,this, namespace, moduleDst);
		if (exist != null)
			module.upgrade(exist);

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

		this.workingDir = workingDir;
		this.distributeDir = distributeDir;
		this.app = app;

		this.loadExistInterfaces(interfacesPath.toFile());
		this.loadExistModules(app, modulePath.toFile());
		startWatch();
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

	private void loadExistModules(AppBase app, File dir) throws Exception {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			var filename = file.getName();
			if (filename.endsWith(".jar")) {
				var namespace = filename.substring(0, filename.indexOf(".jar")); // Temp.jar
				var module = new HotModule(app, this, namespace, file);
				modules.put(namespace, module);
			}

			if (file.isDirectory())
				loadExistModules(app, file);
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
			if (null != loaded)
				return loaded;
		}
		return super.findClass(className);
	}

	private Class<?> loadInterfaceClass(String className, JarFile jar) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
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

		var watcher = FileSystems.getDefault().newWatchService();
		var distributePath = Path.of(distributeDir);
		distributePath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		worker = new Thread(() -> this.watch(watcher));
		worker.setDaemon(true);
		worker.start();;
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

	private void watch(WatchService watcher) {
		try {
			doWatch(watcher);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void doWatch(WatchService watcher) throws Exception {
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
					var ev = (WatchEvent<Path>)event;
					var filename = ev.context().toAbsolutePath(); // 获得稳定的相对路径名。
					if (filename.endsWith(".jar")) {
						System.out.println(filename + " ++++++++++++");
						// todo 检查更新安装包准备好（Module的两个jar都存在），自动调用install。
						// install("");
					}
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
}
