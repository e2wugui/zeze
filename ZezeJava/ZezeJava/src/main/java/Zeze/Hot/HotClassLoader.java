package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class HotClassLoader extends ClassLoader {
	// 目录管理规则（TODO，这个规则是一个限制，但能自动化，是否需要更自由配置方式？）：
	// 1. 目录是一个模块目录时，开启一个新的热更单位；
	// 2. 目录不是模块目录时，它就属于往上级目录方向的最近的热更模块。

	// 模块名字空间，会作为Key加入全局排序HotClassLoader管理器。
	private final String namespace;

	// 本热更单位包含的所有类，如果jar.contains够快，这里维护jar即可，否则装载一次。
	private final HashSet<String> classes = new HashSet<>();
	private final HashSet<String> interfaces = new HashSet<>();
	private final JarFile jar;

	public HotClassLoader(String namespace, File moduleJar) throws IOException {
		this.namespace = namespace;
		this.jar = new JarFile(moduleJar);

		for (var it = jar.entries(); it.hasMoreElements(); ) {
			var e = it.nextElement();
			if (!e.isDirectory() && e.getName().endsWith(".class")) {
				var className = e.getName().replace('/', '.'); // including ".class"
				className = className.substring(0, className.length() - ".class".length()); // remove ".class"

				// todo 打开的时候就全部load一遍，判断是否interface，影响初始化速度。
				//  最好打包的时候预处理，把分类做好保存在Meta里面或者其他更好办法。
				// 现方案希望的结果
				// 0. 不是interface已经装载了，以后loadClass里面的findLoadedClass肯定成功。
				// 1. interface也会被这个ClassLoader装载；这里装载进来仅仅用来判断；
				// 2. 程序里面真正使用的interface.class是parent装载的；
				// ?. 问题：这个loadModuleClass在装载interface时会抛出重复异常。
				if (loadModuleClass(className, e).isInterface())
					interfaces.add(className);
				else
					classes.add(className);
			}
		}
	}

	public String getNamespace() {
		return namespace;
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		if (interfaces.contains(className)) {
			// 虽然是模块定义的，但由外面装载。要求外面的classLoader支持动态增加classpath【确认】。
			return getParent().loadClass(className);
		}

		// 优先load本模块，这个违背了java默认的双亲-Parent优先的规则。
		// 实际上限制了热更模块除了上面的interface特殊处理，它的class不会存在其他地方。
		if (classes.contains(className)) {
			return loadModuleClass(className);
		}

		return getParent().loadClass(className);
	}

	public Class<?> loadModuleClass(String className) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
		return loadModuleClass(className, entry);
	}

	private Class<?> loadModuleClass(String className, ZipEntry entry) {
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

	// start 用来初始化，还没想好可能需要的初始化。
	public void start() {

	}

	// stop 不能清除本地进程状态，后面需要用来升级。
	public void stop() {

	}

	// 先用这个类管理所有热更需求。
	public void upgrade(HotClassLoader old) {
	}
}
