package Zeze.Hot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.IModule;
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

	// module namespace -> HotModule
	private final FewModifySortedMap<String, HotModule> modules = new FewModifySortedMap<>();
	private final ReentrantReadWriteLock hotLock = new ReentrantReadWriteLock();
	private final AppBase app;
	private final HotRedirect hotRedirect;

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

	public String buildCp() throws IOException {
		var sb = new StringBuilder();
		for (var jar : jars.keySet()) {
			sb.append(jar).append(File.pathSeparatorChar);
		}
		for (var module : modules.values()) {
			sb.append(module.getJarFileName()).append(File.pathSeparatorChar);
		}
		for (var path : Reflect.collectClassPaths(ClassLoader.getSystemClassLoader()))
			sb.append(path).append(File.pathSeparatorChar);
		// System.out.println(sb);
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private IModule[] createModuleInstance(Collection<HotModule> result) throws Exception {
		var moduleClasses = new Class[result.size()];
		var i = 0;
		for (var module : result)
			moduleClasses[i++] = module.getModuleClass();
		GenModule.instance.getCompiler().useOptions("-cp", buildCp());
		IModule[] iModules = GenModule.instance.createRedirectModules(app, moduleClasses, hotRedirect);
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

	private List<HotModule> install(List<String> namespaces) throws Exception {
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
			} catch (Exception ignored) {
			}
			//System.out.println("delete interface fail=" + i + ":" + interfaceDstAbsolute);
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
			} catch (Exception ignored) {
			}
			//System.out.println("delete module fail=" + i + ":" + moduleDst);
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
			throw new RuntimeException("distributeDir is sub-dir of workingDir/modulebus/");

		if (Path.of(workingDir).startsWith(distributePath))
			throw new RuntimeException("workingDir is sub-dir of distributeDir");

		if (!Files.isDirectory(distributePath) && GenModule.instance.genFileSrcRoot == null) {
			throw new FileNotFoundException("distributePath = " + distributePath
					+ ", curPath = " + new File(".").getAbsolutePath());
		}

		this.workingDir = workingDir;
		this.distributeDir = distributeDir;
		this.app = app;
		this.hotRedirect = new HotRedirect(this);

		this.loadExistInterfaces(interfacesPath.toFile());
		this.loadExistModules(modulePath.toFile());
		var iModules = createModuleInstance(modules.values());
		var i = 0;
		// 这里要求modules.values()遍历顺序稳定，在modules没有改变时，应该是符合要求的吧。
		for (var module : modules.values()) {
			module.setService(iModules[i++]);
		}

		// todo 先能工作，使用即时命令，可以更快速的得到响应。
		//  准备把 Distribute.java 发展成发布工具。
		//  支持远程，多服务器发布。
		//  支持原子发布。
		var ready = Path.of(distributeDir, "ready");
		Task.schedule(1000, 1000, () -> {
			if (Files.exists(ready) && Files.deleteIfExists(ready))
				installReadies();
		});

		Task.hotGuard = this::enterReadLock;
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

	public void stopModule(String moduleName) throws Exception {
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
		// todo 优化，循环遍历所有jar有点慢。
		//  初始化的时候就读取所有jars.entry并建立统一索引；
		//  索引名字转换成java类名风格；
		//  索引重复的以第一个为准；
		//  jar被替换时需要重建索引；
		//  *重建时直接覆盖索引，不需要删除再加入，因为这里的jar里面的entry只会增加不会删除；
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

	public List<HotModule> installReadies() throws Exception {
		var foundJars = new HashSet<String>();
		var readies = new ArrayList<String>();
		loadExistDistributes(foundJars, readies);
		// todo install order. howto?
		return install(readies);
	}

	private static void tryReady(HashSet<String> foundJars, String jarFileName, ArrayList<String> readies) {
		try {
			final var fileName = jarFileName.substring(0, jarFileName.indexOf(".jar"));
			System.out.println("tryInstall " + fileName);

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

	private void loadExistDistributes(HashSet<String> foundJars, ArrayList<String> readies) {
		var files = new File(distributeDir).listFiles();
		if (null == files) {
			System.out.println("is null.");
			return;
		}

		for (var file : files) {
			System.out.println(file + " " + file.isDirectory());
			if (file.isDirectory())
				continue; // 不支持子目录。

			tryReady(foundJars, file.getName(), readies);
		}
	}
}
