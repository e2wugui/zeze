package Zeze.Services.Log4jQuery.handler;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClassUtils {
	private static final @NotNull Logger logger = LogManager.getLogger(ClassUtils.class);

	/**
	 * 查找包下的所有类(不含内部类)的全名
	 *
	 * @param includeSubPath 是否递归包含子包中的类
	 * @return 类的全名列表
	 */
	public static @NotNull List<String> getClassNames(@NotNull String packageName, boolean includeSubPath) {
		if (!packageName.isEmpty() && packageName.charAt(packageName.length() - 1) != '.')
			packageName += '.';
		var result = new ArrayList<String>();
		try {
			var urls = Thread.currentThread().getContextClassLoader().getResources(packageName.replace('.', '/'));
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				var protocol = url.getProtocol();
				if ("file".equals(protocol))
					getAllClassNameByPath(result, new File(url.getPath()), packageName, includeSubPath);
				else if ("jar".equals(protocol)) {
					getAllClassNameByJar(result, ((JarURLConnection)url.openConnection()).getJarFile(), packageName,
							includeSubPath);
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
