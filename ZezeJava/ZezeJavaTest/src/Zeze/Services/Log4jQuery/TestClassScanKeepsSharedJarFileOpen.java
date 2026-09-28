package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static harness.DirCleanup.deleteBestEffort;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.handler.ClassUtils;

import harness.Fast;

/**
 * FND22 GD-C04回归（FND21 a049278f案外修复残余）：getClassNames对jar协议资源try-with-resources
 * 关闭JarURLConnection.getJarFile()返回的实例——它是JDK全局缓存（JarFileFactory按URL缓存）的
 * 共享实例，close关闭底层zip且不驱散缓存：同JVM第二次扫描从缓存拿到已关闭实例entries()抛
 * IllegalStateException穿透签名；jar部署形态下URLClassPath持同一实例，后续（含懒加载）类加载
 * 读该jar抛zip closed系异常。修复：借来的实例不关闭（生命周期归factory）。
 * 用例构造jar部署形态的最小等价物：URLClassLoader承载jar资源，经与ClassUtils相同的
 * getResources→JarURLConnection通道取回共享实例，断言扫描后实例仍可用。
 */
@Fast
public class TestClassScanKeepsSharedJarFileOpen {
	// 唯一包名：父委托链（测试classpath）上不存在同名包，确保只命中本测试构造的jar。
	private static final String PackageName = "fnd22gd04pkg";

	@Test
	public void testSharedJarFileNotClosedByScan() throws Exception {
		var logDir = Files.createTempDirectory("fnd22-gdc04-jar");
		var jarPath = logDir.resolve("fnd22gd04.jar");
		writeJar(jarPath);
		var previous = Thread.currentThread().getContextClassLoader();
		try (var loader = new URLClassLoader(new URL[] {jarPath.toUri().toURL()})) {
			Thread.currentThread().setContextClassLoader(loader);

			// 与ClassUtils同一通道取回factory缓存的共享实例（getJarFile按jar的URL缓存）。
			var resourceUrl = loader.getResources(PackageName).nextElement();
			assertEquals("jar", resourceUrl.getProtocol(), "前提：包资源应经jar协议枚举");
			var shared = ((JarURLConnection)resourceUrl.openConnection()).getJarFile();

			// 扫描本身正确（无内部类/子包/非class条目混入）。
			var classNames = ClassUtils.getClassNames(PackageName, false);
			assertEquals(List.of(PackageName + ".Foo"), classNames,
					"jar扫描应返回包内顶层类（$内部类、子包、非class条目排除）");

			// 修复点：扫描不关闭共享实例——后续任何持有者（类加载器/第二次扫描）读它不再抛
			// zip closed系异常。修复前try-with-resources已close，entries()/getJarEntry立即失败。
			assertNotNull(assertDoesNotThrow(() -> shared.getJarEntry(PackageName + "/Foo.class")),
					"共享JarFile在扫描后仍可读取条目");
			assertDoesNotThrow(() -> {
				var entries = shared.entries();
				while (entries.hasMoreElements())
					entries.nextElement();
			}, "共享JarFile在扫描后仍可迭代全目录");

			// 同JVM第二次扫描（如ClassUtils.main排障调用）：从缓存拿到同一实例，不再抛
			// IllegalStateException穿透签名。
			assertEquals(List.of(PackageName + ".Foo"), assertDoesNotThrow(
					() -> ClassUtils.getClassNames(PackageName, false)), "第二次扫描同样成功");
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
			deleteBestEffort(logDir);
		}
	}

	private static void writeJar(Path jarPath) throws Exception {
		// 零字节class条目即可：getAllClassNameByJar只读条目名，不解析class内容。
		// 显式目录条目：URLClassPath按目录条目枚举包资源（真实jar工具/Gradle产物均含目录条目）。
		var entries = new ArrayList<String>();
		entries.add(PackageName + "/");
		entries.add(PackageName + "/Foo.class");
		entries.add(PackageName + "/Bar$Inner.class"); // 内部类：$过滤
		entries.add(PackageName + "/sub/Baz.class"); // 子包：includeSubPath=false排除
		entries.add(PackageName + "/readme.txt"); // 非class条目
		try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
			for (var name : entries) {
				jos.putNextEntry(new JarEntry(name));
				jos.closeEntry();
			}
		}
	}
}
