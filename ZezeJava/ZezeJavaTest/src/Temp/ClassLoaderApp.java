package Temp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassLoaderApp {
	public static ClassLoaderApp instance;

	public ModuleA a;
	public ModuleB b;

	public static void test() throws Exception {
		// 静态变量，静态方法的更新。
		// 新旧代码双向是否都能合理的使用。

		ClassLoaderApp.instance = new ClassLoaderApp();
		var a = (IModuleA)ClassLoaderApp.instance.aLoader.loadClass("Temp.ModuleA").getConstructor(int.class).newInstance(1);
		a.callB(ClassLoaderApp.instance.aLoader);
		a.callB(ClassLoaderApp.instance.bLoader);

		/*
		ClassLoaderApp.instance.hotUpdate("Temp.ModuleB", 2);
		ClassLoaderApp.instance.a.callB();
		*/

		ClassLoaderApp.instance = null;
		System.gc();
		System.runFinalization();
		Thread.sleep(1000);
		System.out.println("+++++++++++++");
	}

	static final String classpath = "C:\\code\\zeze\\ZezeJava\\ZezeJavaTest\\build\\classes\\java\\main";
	public ClassLoaderApp() throws Exception {
		aLoader = new ModuleClassLoader(classpath, "Temp.ModuleA");
		bLoader = new ModuleClassLoader(classpath, "Temp.ModuleB");

//		a = (ModuleA)aLoader.loadClass("Temp.ModuleA").getConstructor(int.class).newInstance(1);
//		b = (ModuleB)bLoader.loadClass("Temp.ModuleB").getConstructor(int.class).newInstance(1);
	}

	public void hotUpdate(String moduleName, int version) throws Exception {
		System.out.println("hot update -> " + moduleName);

		if (moduleName.equals("Temp.ModuleA")) {
			aLoader = new ModuleClassLoader(classpath, "Temp.ModuleA");
			a = (ModuleA)aLoader.loadClass("Temp.ModuleA").getConstructor(int.class).newInstance(version);

			return;
		}

		if (moduleName.equals("Temp.ModuleB")) {
			bLoader = new ModuleClassLoader(classpath, "Temp.ModuleB");
			b = (ModuleB)bLoader.loadClass("Temp.ModuleB").getConstructor(int.class).newInstance(version);
		}
	}

	public ModuleClassLoader aLoader;
	public ModuleClassLoader bLoader;

	public static class ModuleClassLoader extends ClassLoader {
		private final String classpath;
		private final String moduleName;

		public ModuleClassLoader(String classpath, String moduleName) {
			this.classpath = classpath;
			this.moduleName = moduleName;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.startsWith("Temp.")) {

				if (name.endsWith("ModuleClassLoader") || name.endsWith(".IModuleA")) {
					System.out.println("super.loadClass " + name + "@" + this);
					return super.loadClass(name);
				}
				var file = getClassFile(name);
				System.out.println("try load " + file);
				try {
					byte[] classBytes = Files.readAllBytes(Path.of(file));
					var define = defineClass(name, classBytes, 0, classBytes.length);
					if (null != define) {
						System.out.println("load success. " + name);
						return define;
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return super.loadClass(name);
		}

		private String getClassFile(String name) {
			return classpath + "/" +
					name.replace('.', '/') +
					".class";
		}

		@Override
		protected void finalize() {
			System.out.println("~ModuleClassLoader" + this);
		}

		@Override
		public String toString() {
			return "ClassLoader: " + moduleName;
		}
	}
}
