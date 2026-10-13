package Zeze.Services.Log4jQuery.handler;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 类扫描工具：按包名收集类全名，支持目录与 jar 两种 classpath 形态。
 */
public class ClassUtils {
	private static final @NotNull Logger logger = LogManager.getLogger(ClassUtils.class);

	/**
	 * 查找包下的所有类(不含内部类)的全名
	 *
	 * @param includeSubPath 是否递归包含子包中的类
	 * @return 类的全名列表
	 */
	// synchronized：JarURLConnection.getJarFile() 返回 JDK 全局缓存的共享 JarFile，
	// 双扫描对同一实例的并发迭代受JarFile内部同步保护，但为防未来回归仍串行化调用方
	//（首次触发 QueryHandlerManager.<clinit> 的并发会把该类永久打成 NoClassDefFound）。
	public static synchronized @NotNull List<String> getClassNames(@NotNull String packageName, boolean includeSubPath) {
		if (!packageName.isEmpty() && packageName.charAt(packageName.length() - 1) != '.')
			packageName += '.';
		var result = new ArrayList<String>();
		try {
			var urls = Thread.currentThread().getContextClassLoader().getResources(packageName.replace('.', '/'));
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				var protocol = url.getProtocol();
				if ("file".equals(protocol)) {
					try {
						// 必须经URI解码：url.getPath()带百分号编码（空格=%20），classpath含空格/中文时new File得到错误路径，扫描静默为空。
						getAllClassNameByPath(result, new File(url.toURI()), packageName, includeSubPath);
					} catch (URISyntaxException e) {
						logger.error("invalid file url: {}", url, e);
					}
				} else if ("jar".equals(protocol)) {
					// getJarFile()返回JDK全局缓存（JarFileFactory按URL缓存）的共享实例：
					// 借来的实例不关闭——try-with-resources的close会关闭底层zip且不驱散factory缓存，
					// jar部署形态下URLClassPath的JarLoader持同一实例，后续（含懒加载）类加载读该jar
					// 抛zip closed系异常，init的catch吞掉后handler静默缺失；同JVM第二次扫描从缓存拿到
					// 已关闭实例entries()直接抛IllegalStateException穿透签名。生命周期归factory自持有。
					// synchronized保留：消除本方法并发双扫的迭代/关闭竞速。
					var jarFile = ((JarURLConnection)url.openConnection()).getJarFile();
					getAllClassNameByJar(result, jarFile, packageName, includeSubPath);
				}
			}
		} catch (IOException e) {
			logger.error("getClassName exception:", e);
		}
		return result;
	}

	/**
	 * 从指定目录中获取所有class文件的类名(不含内部类)
	 *
	 * @param includeSubPath 是否递归包含子包中的类
	 */
	private static void getAllClassNameByPath(@NotNull List<String> result, @NotNull File path,
											  @NotNull String packageName, boolean includeSubPath) {
		var listFiles = path.listFiles();
		if (listFiles != null) {
			for (File file : listFiles) {
				if (file.isFile()) {
					var fileName = file.getName();
					if (fileName.endsWith(".class") && fileName.indexOf('$') < 0 && fileName.indexOf('-') < 0)
						result.add(packageName + fileName.substring(0, fileName.length() - ".class".length()));
				} else if (includeSubPath)
					getAllClassNameByPath(result, file, packageName + file.getName() + '.', true);
			}
		}
	}

	/**
	 * 从指定jar文件中的包名下获取所有class文件的类名(不含内部类)
	 *
	 * @param includeSubPath 是否递归包含子包中的类
	 */
	private static void getAllClassNameByJar(@NotNull List<String> result, @NotNull JarFile jarFile,
											 @NotNull String packageName, boolean includeSubPath) {
		int n = packageName.length();
		for (var e = jarFile.entries(); e.hasMoreElements(); ) {
			var filePath = e.nextElement().getName();
			if (filePath.endsWith(".class")) {
				filePath = filePath.substring(0, filePath.length() - ".class".length())
						.replace('\\', '.').replace('/', '.');
				if (filePath.startsWith(packageName) && filePath.indexOf('$') < 0 && filePath.indexOf('-') < 0
						&& (includeSubPath || filePath.lastIndexOf(".") < n))
					result.add(filePath);
			}
		}
	}

	public static void main(String[] args) {
		getClassNames(args[0], args.length > 1).forEach(System.out::println);
	}
}
