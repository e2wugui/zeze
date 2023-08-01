package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 装载所有的模块接口。
 *
 * 1. 会启动一个线程检测dir，自动装载新加入jar。
 * 2. 一般全局一个实例。
 * 3. jar覆盖的时候，能装载里面新加入的class，但是同名的已经loadClass的类不会改变。
 */
public class HotManager extends ClassLoader {
	private final Path directory;

	private final FewModifyMap<Path, JarFile> jars = new FewModifyMap<>();
	private volatile boolean running = true;
	private final Thread worker;

	// module namespace -> HotModule
	private final FewModifySortedMap<String, HotModule> modules = new FewModifySortedMap<>();

	// 采用其他管理措施以后，这个方法很可能不需要了。
	public HotModule find(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : modules.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null; // throw ?
	}

	public HotModule getModule(String moduleNamespace) {
		return modules.get(moduleNamespace);
	}

	public HotModule put(String namespace, File jarFile) throws Exception {
		var module = new HotModule(this, namespace, jarFile);

		// todo 生命期管理，确定服务是否可用，等等。
		var exist = modules.get(module.getName());
		if (exist == null) {
			module.start();
			modules.put(module.getName(), module);
			return module;
		}
		// upgrade(exist, module);
		exist.stop();
		module.upgrade(exist);
		module.start();
		modules.put(module.getName(), module);
		return module;
	}

	public HotManager(Path directory) throws IOException {
		this.directory = directory;

		// 1. 先创建注册
		// 2. 装载已经存在的jar
		// 3. 监视线程启动
		// 4. 【按这个顺序】有可能重复打开同一个jar，但可以避免丢失
		var watcher = FileSystems.getDefault().newWatchService();
		directory.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		loadExistJar(directory.toFile());
		worker = new Thread(() -> this.watch(watcher));
		worker.setDaemon(true);
		worker.start();;
	}

	private void loadExistJar(File dir) throws IOException {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.getName().endsWith(".jar"))
				jars.put(file.getAbsoluteFile().toPath(), new JarFile(file));

			if (file.isDirectory())
				loadExistJar(file);
		}
	}

	public Path getDirectory() {
		return directory;
	}

	// 严格的话，最好调用这个停止监视线程。
	public void stopAndJoin() throws InterruptedException {
		running = false;
		worker.join();
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
		var loaded = findLoadedClass(className);
		if (null != loaded)
			return loaded;
		try (var inputStream = jar.getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			return defineClass(className, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
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

				// The filename is the context of the event.
				var ev = (WatchEvent<Path>)event;
				var filename = ev.context().toAbsolutePath(); // 获得稳定的相对路径名。
				if (filename.endsWith(".jar")) {
					System.out.println(filename + " ++++++++++++");
					jars.put(filename, new JarFile(filename.toFile()));
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
