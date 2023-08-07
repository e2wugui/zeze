package Zeze.Util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
MANIFEST.MF 需要定义以下2行:
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

	public static Instrumentation getInstrumentation() {
		return inst;
	}

	/** Java Agent 入口 */
	public static void premain(@SuppressWarnings("unused") String args, Instrumentation inst) {
		ClassReloader.inst = inst;
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
			// System.out.println(String.format("%6X: %4d/%4d = %d", classData.length - dis.available(), i + 1, constCount, t));
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
	public static void reloadClass(byte @NotNull [] classData) throws Exception {
		if (inst == null)
			throw new IllegalStateException("Instrumentation not initialized");
		inst.redefineClasses(new ClassDefinition(Class.forName(getClassPathFromData(classData)), classData));
	}

	/** 批量热更多个class数据 */
	public static void reloadClasses(@NotNull Collection<byte[]> classDatas) throws Exception {
		if (inst == null)
			throw new IllegalStateException("Instrumentation not initialized");
		int i = 0, n = classDatas.size();
		ClassDefinition[] clsDefs = new ClassDefinition[n];
		for (byte[] classData : classDatas)
			clsDefs[i++] = new ClassDefinition(Class.forName(getClassPathFromData(classData)), classData);
		inst.redefineClasses(clsDefs);
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
		if (inst == null)
			throw new IllegalStateException("Instrumentation not initialized");
		ArrayList<byte[]> classDatas = new ArrayList<>();
		ByteBuffer buf0 = ByteBuffer.Allocate(), buf1 = ByteBuffer.Allocate();
		for (Enumeration<? extends ZipEntry> zipEnum = zipFile.entries(); zipEnum.hasMoreElements(); ) {
			ZipEntry ze = zipEnum.nextElement();
			String name;
			if (ze.isDirectory() || !(name = ze.getName()).endsWith(".class"))
				continue;
			buf1.Reset();
			try (InputStream is = zipFile.getInputStream(ze)) {
				readStream(is, buf1);
			}
			if (buf1.isEmpty())
				continue;
			if (classLoader != null) {
				try (InputStream is = classLoader.getResourceAsStream(name)) {
					if (is != null) {
						buf0.Reset();
						readStream(is, buf0);
						if (buf0.equals(buf1))
							continue;
					}
				}
			}
			if (log != null)
				log.append(name).append('\n');
			classDatas.add(buf1.Copy());
		}
		reloadClasses(classDatas);
		return classDatas.size();
	}

	/** 从输入流中读取未知长度的数据,一直取到无法获取为止 */
	public static @NotNull ByteBuffer readStream(@NotNull InputStream is, @Nullable ByteBuffer bb) throws IOException {
		if (bb == null)
			bb = ByteBuffer.Allocate();
		for (int wi = bb.WriteIndex; ; ) {
			bb.EnsureWrite(8192);
			byte[] buf = bb.Bytes;
			int n = is.read(buf, wi, buf.length - wi);
			if (n <= 0) {
				bb.WriteIndex = wi;
				break;
			}
			wi += n;
		}
		return bb;
	}
}
