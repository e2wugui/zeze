package Zeze.Util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import Zeze.Serialize.ByteBuffer;
import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
MANIFEST.MF 需要定义以下2行("Premain-Class"可换成"Agent-Class"):
Premain-Class: Zeze.Util.ClassReloader
Can-Redefine-Classes: true

运行参数:
java ...... -javaagent:zeze.jar ......

注意: javac和Eclipse编译器二者的编译结果可能不兼容
*/
public final class ClassReloader {
	private static Instrumentation inst;

	private ClassReloader() {
	}

	/** Java Agent 入口(通过Premain-Class) */
	public static void premain(@SuppressWarnings("unused") String args, Instrumentation inst) {
		ClassReloader.inst = inst;
	}

	/** Java Agent 入口(通过Agent-Class) */
	public static void agentmain(String args, Instrumentation inst) {
		premain(args, inst);
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.exit(-1);
			return;
		}
		try {
			VirtualMachine vm = VirtualMachine.attach(args[0]);
			vm.loadAgent(args[1], null);
			vm.detach();
		} catch (Throwable e) {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			System.exit(-2);
		}
	}

	public static Instrumentation getInst() {
		return inst != null ? inst : loadAgent();
	}

	private static Instrumentation loadAgent() {
		try {
			String fullClassName = ClassReloader.class.getName();
			File agentJar = File.createTempFile("agent", ".jar");
			agentJar.deleteOnExit();
			Manifest manifest = new Manifest();
			Attributes attrs = manifest.getMainAttributes();
			attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			attrs.put(new Attributes.Name("Agent-Class"), fullClassName);
			attrs.put(new Attributes.Name("Premain-Class"), fullClassName);
			attrs.put(new Attributes.Name("Can-Redefine-Classes"), "true");
			attrs.put(new Attributes.Name("Can-Retransform-Classes"), "true");
			try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(agentJar), manifest)) {
				String classFileName = fullClassName.replace('.', '/') + ".class";
				jos.putNextEntry(new JarEntry(classFileName));
				try (InputStream in = ClassReloader.class.getClassLoader().getResourceAsStream(classFileName)) {
					assert in != null;
					jos.write(in.readAllBytes());
				}
				jos.closeEntry();
			}
			String path = agentJar.getAbsolutePath();
			String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
			String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
			int r = Runtime.getRuntime().exec(new String[]{"java", "-cp", path, fullClassName, pid, path}).waitFor();
			if (r != 0)
				throw new IllegalStateException("loadAgent process = " + r);
		} catch (Exception e) {
			Task.forceThrow(e);
		}
		return inst;
	}

	/** 从class数据里获取完整类名 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String getClassPathFromData(byte @NotNull [] classData) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(classData));
		dis.readLong(); // skip magic[4] and version[4]
		int constCount = dis.readUnsignedShort() - 1;
		int[] classes = new int[constCount];
		String[] strings = new String[constCount];
		for (int i = 0; i < constCount; i++) {
			int t = dis.read();
			switch (t) {
			case 1: // CONSTANT_Utf8
				strings[i] = dis.readUTF();
				break;
			case 7: // CONSTANT_Class
				classes[i] = dis.readUnsignedShort();
				break;
			default: // others(3,4,9,10,11,12,17,18) (4 bytes)
				dis.read();
				//noinspection fallthrough
			case 15: // CONSTANT_MethodHandle
				dis.read();
				//noinspection fallthrough
			case 8: // CONSTANT_String
			case 16: // CONSTANT_MethodType
			case 19: // CONSTANT_Module
			case 20: // CONSTANT_Package
				dis.read();
				dis.read();
				break;
			case 5: // CONSTANT_Long
			case 6: // CONSTANT_Double
				dis.readLong();
				i++;
				break;
			}
		}
		dis.read(); // skip access flags
		dis.read();
		return strings[classes[dis.readUnsignedShort() - 1] - 1].replace('/', '.');
	}

	/** 热更一个class数据 */
	public static void reloadClass(byte @NotNull [] classData, @Nullable ClassLoader classLoader) throws Exception {
		String fullClassName = getClassPathFromData(classData);
		Class<?> cls = classLoader != null ?
				Class.forName(fullClassName, true, classLoader) : Class.forName(fullClassName);
		getInst().redefineClasses(new ClassDefinition(cls, classData));
	}

	/** 批量热更多个class数据 */
	public static void reloadClasses(@NotNull Collection<byte[]> classDatas, @Nullable ClassLoader classLoader)
			throws Exception {
		int i = 0, n = classDatas.size();
		ClassDefinition[] clsDefs = new ClassDefinition[n];
		for (byte[] classData : classDatas) {
			String fullClassName = getClassPathFromData(classData);
			Class<?> cls = classLoader != null ?
					Class.forName(fullClassName, true, classLoader) : Class.forName(fullClassName);
			clsDefs[i++] = new ClassDefinition(cls, classData);
		}
		getInst().redefineClasses(clsDefs);
	}

	/**
	 * @param zipFile 指定zip/jar文件,热更里面所有的class文件
	 * @return 批量热更的class数量
	 */
	public static int reloadClasses(@NotNull ZipFile zipFile) throws Exception {
		return reloadClasses(zipFile, null, null);
	}

	/**
	 * @param zipFile     指定zip/jar文件,热更里面所有的class文件
	 * @param classLoader 如果不为null,则会对比classLoader里的class文件,如果一致则忽略热更
	 * @param log         如果不为null,则会追加热更的class文件名
	 * @return 批量热更的class数量
	 */
	public static int reloadClasses(@NotNull ZipFile zipFile, @Nullable ClassLoader classLoader,
									@Nullable Appendable log) throws Exception {
		ArrayList<byte[]> classDatas = new ArrayList<>();
		ByteBuffer buf0 = ByteBuffer.Allocate(), buf1 = ByteBuffer.Allocate();
		for (Enumeration<? extends ZipEntry> zipEnum = zipFile.entries(); zipEnum.hasMoreElements(); ) {
			ZipEntry ze = zipEnum.nextElement();
			String name;
			if (ze.isDirectory() || !(name = ze.getName()).endsWith(".class"))
				continue;
			buf1.Reset();
			try (InputStream is = zipFile.getInputStream(ze)) {
				buf1.readStream(is);
			}
			if (buf1.isEmpty())
				continue;
			if (classLoader != null) {
				try (InputStream is = classLoader.getResourceAsStream(name)) {
					if (is != null) {
						buf0.Reset();
						buf0.readStream(is);
						if (buf0.equals(buf1))
							continue;
					}
				}
			}
			if (log != null)
				log.append(name).append('\n');
			classDatas.add(buf1.Copy());
		}
		reloadClasses(classDatas, classLoader);
		return classDatas.size();
	}
}
