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

	public <T extends HotService> HotModuleContext<T> getModuleContext(String moduleNamespace) {
		var module = modules.get(moduleNamespace);
		if (null == module)
			throw new RuntimeException("module not exist. " + moduleNamespace);
		return module.<T>createContext();
	}

	public List<HotModule> install(List<String> namespaces) throws Exception {
		try (var ignored = enterWriteLock()) {
			var result = new ArrayList<HotModule>();
			for (var namespace : namespaces)
				result.add(_install(namespace));
			for (var module : result)
				module.start();
			return result;
		}
	}

	public HotModule install(String namespace) throws Exception {
		try (var ignored = enterWriteLock()) {
			var module = _install(namespace);
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
		var oldI = jars.remove(interfaceDstAbsolute);
		if (null != oldI)
			oldI.close();
		Files.deleteIfExists(interfaceDstAbsolute.toPath());
		if (!interfaceSrc.renameTo(interfaceDstAbsolute))
			throw new RuntimeException("rename fail. " + interfaceSrc + "->" + interfaceDstAbsolute);
		jars.put(interfaceDstAbsolute, new JarFile(interfaceDstAbsolute));

		// 安装 module
		HotModule exist = modules.remove(namespace);
		var moduleDst = Path.of(workingDir, "modules", namespace + ".jar").toFile();
		if (exist != null)
			exist.stop();
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
		this.workingDir = workingDir;
		this.distributeDir = distributeDir;
		this.app = app;

		// todo 检查 distributeDir 不能是 workingDir/interfaces/ 的子目录。
		// todo 检查 distributeDir 不能是 workingDir/modules/ 的子目录。
		// todo 检查 workingDir 不能是 distributeDir 的子目录。

		this.loadExistInterfaces(Path.of(workingDir, "interfaces").toFile());
		this.loadExistModules(app, Path.of(workingDir, "modules").toFile());
		startWatch();
	}

	public void startModules(List<String> startOrder) throws Exception {
		// todo 根据solution定义的顺序启动。
		for (var module : modules.values())
			module.start();
	}

	public void stopModules(List<String> stopOrder) throws Exception {
		// todo 根据solution定义的顺序启动。
		for (var module : modules.values())
			module.stop();
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
