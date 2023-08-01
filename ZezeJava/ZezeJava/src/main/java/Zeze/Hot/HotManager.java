package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;

/**
 * 装载所有的模块接口。
 *
 * 1. 一般全局一个实例。
 * 2. jar覆盖的时候，能装载里面新加入的class，但是同名的已经loadClass的类不会改变。
 */
public class HotManager extends ClassLoader {
	private final Path directory;

	private final FewModifyMap<File, JarFile> jars = new FewModifyMap<>();

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

	public HotModule install(Path dir, String namespace) throws Exception {
		// 使用临时文件名拷贝文件到工作目录。后面rename，减少锁定时间。
		var moduleSrc = Path.of(dir.toString(), namespace + ".jar");
		var moduleTmp = Path.of(directory.toString(), "modules", namespace + ".install.jar");
		var interfaceSrc = Path.of(dir.toString(), namespace + ".Interface.jar");
		var interfaceTmp = Path.of(directory.toString(), "interfaces", namespace + ".Interface.install.jar");
		Files.copy(moduleSrc, moduleTmp);
		Files.copy(interfaceSrc, interfaceTmp);

		// todo 同步方式需要修改成读写锁，跟系统运行互斥。
		synchronized (this) {
			// todo 生命期管理，确定服务是否可用，等等。
			// 安装 interface
			var interfaceDstAbsolute = Path.of(directory.toString(), "interfaces", namespace + ".Interface.jar")
					.toFile().getAbsoluteFile();
			var oldI = jars.remove(interfaceDstAbsolute);
			if (null != oldI)
				oldI.close();
			Files.deleteIfExists(interfaceDstAbsolute.toPath());
			if (!interfaceTmp.toFile().renameTo(interfaceDstAbsolute))
				throw new RuntimeException("rename fail. " + interfaceTmp + "->" + interfaceDstAbsolute);
			jars.put(interfaceDstAbsolute, new JarFile(interfaceDstAbsolute));

			// 安装 module
			HotModule exist = modules.remove(namespace);
			var moduleDst = Path.of(directory.toString(), "modules", namespace + ".jar").toFile();
			if (exist != null)
				exist.stop();
			Files.deleteIfExists(moduleDst.toPath());
			if (!moduleTmp.toFile().renameTo(moduleDst))
				throw new RuntimeException("rename fail. " + moduleTmp + "->" + moduleDst);
			var module = new HotModule(this, namespace, moduleDst);
			if (exist != null)
				module.upgrade(exist);

			module.start();
			modules.put(module.getName(), module);
			return module;
		}
	}

	public HotManager(Path directory) throws Exception {
		this.directory = directory;
		this.loadExistJar(directory.toFile());
	}

	private void loadExistJar(File dir) throws IOException {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.getName().endsWith(".jar"))
				jars.put(file.getAbsoluteFile(), new JarFile(file));

			if (file.isDirectory())
				loadExistJar(file);
		}
	}

	public Path getDirectory() {
		return directory;
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
}
