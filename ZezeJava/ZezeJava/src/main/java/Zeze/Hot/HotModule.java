package Zeze.Hot;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
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
public class HotModule extends ClassLoader implements Closeable {
	private static final Logger logger = LogManager.getLogger(HotModule.class);
	private final File jarFile;
	private JarFile jar; // 模块的class（interface除外）必须打包成一个jar，只支持一个。
	private final Class<?> moduleClass;
	private HotService service;
	private final boolean isLoadSchemas;

	// 每个版本的接口一个上下文。
	private final ConcurrentHashMap<Class<?>, HotModuleContext<?>> contexts = new ConcurrentHashMap<>();
	public final ConcurrentHashSet<Action1<HotModule>> stopEvents = new ConcurrentHashSet<>();

	// 为了支持批量装载redirect.class，构造只初始化moduleClass，service下一步处理。
	public HotModule(HotManager parent, String namespace, File jarFile) throws Exception {
		super(namespace, parent);
		this.jarFile = jarFile;
		this.jar = new JarFile(jarFile);
		// App.ModuleClassName：MySolution.MyName.ModuleMyName，namespace=MySolution.MyName
		// MyName 一般就叫模块名字。
		var moduleClassName = namespace + ".Module" + last(namespace);
		this.moduleClass = loadClass(moduleClassName);
		this.isLoadSchemas = false;
	}

	// 用于装载 Schemas. 借用这个类实现单独的装载。
	HotModule(File jarFile) throws Exception {
		this.jarFile = jarFile;
		this.jar = new JarFile(jarFile);
		this.moduleClass = null;
		this.isLoadSchemas = true;
	}

	public String getJarFileName() {
		return jar.getName();
	}

	JarFile getJarFile() {
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
		if (jar != null) {
			jar.close();
			jar = null;
		}
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
		return isLoadSchemas && className.equals("Game.Schemas")
				? loadModuleClass(className)
				: super.loadClass(className, resolve);
	}

	@Override
	protected Class<?> findClass(String className) {
		return loadModuleClass(className);
	}

	private Class<?> loadModuleClass(String className) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
		if (entry == null)
			logger.error("loadModuleClass: not found entry: '{}'", classFileName);
		return loadModuleClass(className, entry);
	}

	private Class<?> loadModuleClass(String className, ZipEntry entry) {
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

	public static final String eModuleConfigName = "META-INF/module.config";

	public BModule.Data loadModuleConfig() throws Exception {
		var entry = jar.getEntry(eModuleConfigName);
		try (var inputStream = jar.getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			var bbConfig = ByteBuffer.Wrap(bytes);
			var config = new BModule.Data();
			config.decode(bbConfig);
			return config;
		}
	}

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
