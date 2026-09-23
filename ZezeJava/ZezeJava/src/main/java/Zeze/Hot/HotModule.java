package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import Zeze.Arch.Gen.GenModule;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action1;
import Zeze.Util.ConcurrentHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// 目录管理规则
// 1. 目录是一个模块目录时，开启一个新的热更单位；
// 2. 目录不是模块目录时，它就属于往上级目录方向的最近的热更模块。
// close() 只释放jar句柄（Schemas装载器靠它收尾，见HotManager.loadSchemas），
// 不是模块停机语义；模块的完整停机走 stop()。
public class HotModule extends ClassLoader implements AutoCloseable, GenModule.RedirectClassSink {
	private static final Logger logger = LogManager.getLogger(HotModule.class);
	private final File jarFile;
	private JarFile jar; // 模块的class（interface除外）必须打包成一个jar，只支持一个。
	private final Class<?> moduleClass;
	private HotService service;
	private final boolean isLoadSchemas;
	// isLoadSchemas时强制从本jar装载的类名（solutionName + ".Schemas"）。
	// solution名不一定是"Game"，不能硬编码：写死会把非Game命名的solution的
	// Schemas类走双亲委派命中冷类路径上的旧类，schema热更静默失效。
	private final String schemasClassName;

	// 每个版本的接口一个上下文。
	private final ConcurrentHashMap<Class<?>, HotModuleContext<?>> contexts = new ConcurrentHashMap<>();
	public final ConcurrentHashSet<Action1<HotModule>> stopEvents = new ConcurrentHashSet<>();

	// 为了支持批量装载redirect.class，构造只初始化moduleClass，service下一步处理。
	public HotModule(HotManager parent, String namespace, File jarFile) throws Exception {
		super(namespace, parent);
		this.jarFile = jarFile;
		//this.jar = new JarFile(jarFile);
		// App.ModuleClassName：MySolution.MyName.ModuleMyName，namespace=MySolution.MyName
		// MyName 一般就叫模块名字。
		var moduleClassName = namespace + ".Module" + last(namespace);
		this.moduleClass = loadClass(moduleClassName);
		// 双亲委派下，冷classpath上的同名类会抢先命中；冷类不受热更控制——之后换jar
		// 也不会换掉这个身份，安装对该模块静默失效。fail-fast发生在_install可回滚区，
		// 走既有recoverModules恢复。
		if (moduleClass.getClassLoader() != this)
			throw new IllegalStateException("hot module shadowed by cold classpath: "
					+ moduleClassName + " loaded by " + moduleClass.getClassLoader());
		this.isLoadSchemas = false;
		this.schemasClassName = null;
	}

	// 用于装载 Schemas. 借用这个类实现单独的装载。
	// schemasClassName：期望从本jar装载的Schemas类名，loadClass对它绕过双亲委派。
	HotModule(File jarFile, String schemasClassName) throws Exception {
		this.jarFile = jarFile;
		this.jar = new JarFile(jarFile);
		this.moduleClass = null;
		this.isLoadSchemas = true;
		this.schemasClassName = schemasClassName;
	}

	public String getJarFileName() {
		return jarFile.getName();
	}

	JarFile getJarFile() throws IOException {
		if (null == jar)
			jar = new JarFile(jarFile);
		return jar;
	}

	Class<?> getModuleClass() {
		return moduleClass;
	}

	void setService(IModule service) {
		this.service = (HotService)service;
	}

	private static String last(String namespace) {
		var ns = namespace.split("\\.");
		return ns[ns.length - 1];
	}

	@SuppressWarnings("unchecked")
	public <T extends HotService> HotModuleContext<T> getContext(Class<T> serviceClass) {
		return (HotModuleContext<T>)contexts.computeIfAbsent(serviceClass, (key) -> new HotModuleContext<T>(this));
	}

	public HotService getService() {
		return service;
	}

	// start 用来初始化，还没想好可能需要的初始化。
	public void start() throws Exception {
		if (null == this.jar)
			this.jar = new JarFile(jarFile);
		service.start();
		// 安装过程中可能需要重启，因为停止时清除了引用，这里需要重新设置。
		for (var context : contexts.values()) {
			context.setModule(this);
		}
	}

	public void startLast() throws Exception {
		service.startLast();
	}

	void disable() {
		// 停止不允许失败，首先去掉旧的引用。
		for (var context : contexts.values()) {
			context.setModule(null);
		}
	}

	// 内部关联停止，不可恢复。
	void stopInternal() {
		disable();
		// 停止事件。
		for (var stopEvent : stopEvents) {
			try {
				stopEvent.run(this);
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
		stopEvents.clear();
	}

	// stop 不能清除本地进程状态，后面需要用来升级。
	public void stop() throws Exception {
		// app Unregister
		var iModule = (IModule)service;
		iModule.UnRegister();
		// app stop
		service.stop();
		close();
	}

	public void stopBefore() throws Exception {
		service.stopBefore();
	}

	// 先用这个类管理所有热更需求。
	public void upgrade(HotModule old) throws Exception {
		contexts.putAll(old.contexts);
		for (var context : contexts.values()) {
			context.setModule(this);
		}
		service.upgrade(old.service);
	}

	@Override
	public Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
		return isLoadSchemas && className.equals(schemasClassName)
				? loadModuleClass(className)
				: super.loadClass(className, resolve);
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		return loadModuleClass(className);
	}

	// 装载jar里打包好的Redirect_子类（Distribute.pack产出）。必须绕过双亲委派只查本jar：
	// 冷classpath上的陈旧同名子类可经parent委派抢先，装载到旧身份。
	@Override
	public Class<?> findRedirectClass(String className) throws ClassNotFoundException {
		return loadModuleClass(className);
	}

	// 兜底编译产物define进本HotModule：每模块版本一份，换代即隔离；
	// 安装失败回滚时随本实例一起废弃，不在共享装载器留残留。
	@Override
	public Class<?> defineRedirectClass(String className, byte[] byteCode) {
		var loaded = findLoadedClass(className);
		if (loaded != null)
			return loaded;
		return defineClass(className, byteCode, 0, byteCode.length);
	}

	private Class<?> loadModuleClass(String className) throws ClassNotFoundException {
		String classFileName = className.replace('.', '/') + ".class";
		ZipEntry entry;
		try {
			// 构造器会调loadClass，此时jar可能尚未打开（7e8403ab8延迟打开），必须走lazy的getJarFile
			entry = getJarFile().getEntry(classFileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (entry == null)
			// 必须按loadClass契约抛ClassNotFoundException而不是继续走下去：
			// getInputStream(null)只会得到NullPointerException，调用方按CNFE写的
			// 回退逻辑（拼错类名、反射探测、编译器探测）全部失效。
			throw new ClassNotFoundException(className);
		return loadModuleClass(className, entry);
	}

	private Class<?> loadModuleClass(String className, ZipEntry entry) {
		// 采用标准方式重载findClass以后，不需要判断这个了。
//		var loaded = findLoadedClass(className);
//		if (null != loaded)
//			return loaded;
		try (var inputStream = getJarFile().getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			return defineClass(className, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static final String eModuleConfigName = "META-INF/module.config";

	public BModule.Data loadModuleConfig() throws Exception {
		var entry = getJarFile().getEntry(eModuleConfigName);
		// entry缺失必须抛带定位信息的异常而不是getInputStream(null)的裸NPE：
		// 本调用位于install不可回滚区（addHotModule注册链），裸NPE会被catch(Throwable)升级为halt。
		if (entry == null)
			throw new IllegalStateException("module.config not found in jar. jar=" + getName());
		try (var inputStream = getJarFile().getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			var bbConfig = ByteBuffer.Wrap(bytes);
			var config = new BModule.Data();
			config.decode(bbConfig);
			return config;
		}
	}

	// 只释放jar句柄（幂等），不停service；模块停机用stop()。
	@Override
	public void close() throws IOException {
		if (jar != null) {
			jar.close();
			jar = null;
		}
	}

	@Override
	public String toString() {
		return getName();
	}
}
