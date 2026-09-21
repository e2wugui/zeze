package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Compile Java sources in-memory (not thread safe for one instance)
 * from <a href="https://github.com/trung/InMemoryJavaCompiler">GitHub</a>
 */
public class InMemoryJavaCompiler {
	private final JavaCompiler javac;
	private DynamicClassLoader classLoader;
	private Iterable<String> options;
	private final ArrayList<SourceCode> sourceCodes = new ArrayList<>();
	private int ignoreWarningLevel = 2; // 0:>=OTHER 1:>=NOTE 2:>=MANDATORY_WARNING 3:>=WARNING 4:>=ERROR

	public InMemoryJavaCompiler() {
		this(ClassLoader.getSystemClassLoader());
	}

	public InMemoryJavaCompiler(ClassLoader parent) {
		javac = ToolProvider.getSystemJavaCompiler();
		classLoader = new DynamicClassLoader(parent);
	}

	/**
	 * @return the class loader used internally by the compiler
	 */
	public ClassLoader getClassloader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		if (!(classLoader instanceof DynamicClassLoader))
			throw new IllegalArgumentException("classLoader is not DynamicClassLoader");
		this.classLoader = (DynamicClassLoader)classLoader;
	}

	public InMemoryJavaCompiler useParentClassLoader(ClassLoader parent) {
		classLoader = new DynamicClassLoader(parent);
		return this;
	}

	/**
	 * Options used by the compiler, e.g. '-Xlint:unchecked'.
	 */
	public InMemoryJavaCompiler useOptions(String... options) {
		this.options = Arrays.asList(options);
		return this;
	}

	/**
	 * Ignore non-critical compiler output, like unchecked/unsafe operation warnings.
	 */
	public InMemoryJavaCompiler ignoreWarnings() {
		ignoreWarningLevel = 4;
		return this;
	}

	public InMemoryJavaCompiler setIgnoreWarningLevel(int level) {
		ignoreWarningLevel = level;
		return this;
	}

	public InMemoryJavaCompiler addSource(String className, String sourceCode) {
		sourceCodes.add(new SourceCode(className, sourceCode));
		return this;
	}

	/**
	 * @return warning/error message or null for compiling success
	 */
	public String compileAll() {
		if (sourceCodes.isEmpty())
			return null;
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
		// try-with-resources 确保 close 释放（close 声明抛 IOException，需在此捕获）
		try (var fm = new ExtendedJavaFileManager(
				javac.getStandardFileManager(null, null, null), classLoader)) {
			javac.getTask(null, fm, collector, options, null, sourceCodes).call();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (!collector.getDiagnostics().isEmpty()) {
			StringBuilder exceptionMsg = new StringBuilder("Unable to compile the source");
			int warningLevel = 0;
			for (Diagnostic<?> d : collector.getDiagnostics()) {
				warningLevel = switch (d.getKind()) {
					case OTHER -> Math.max(warningLevel, 1);
					case NOTE -> Math.max(warningLevel, 2);
					case MANDATORY_WARNING -> Math.max(warningLevel, 3);
					case WARNING -> Math.max(warningLevel, 4);
					default -> 5; // ERROR
				};
				exceptionMsg.append('\n').append("[kind=").append(d.getKind());
				var source = d.getSource();
				if (source instanceof SourceCode)
					exceptionMsg.append(", ").append("class=").append(((SourceCode)source).getClassName());
				exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
				exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append(']');
			}
			if (ignoreWarningLevel < warningLevel)
				return exceptionMsg.toString();
		}
		return null;
	}

	// 同实例对已define名字的二次编译：defineCompiled直调findClass（不经loadClass的
	// findLoadedClass缓存），重复defineClass必LinkageError，二次编译的字节码还与已
	// 定义身份错配——编译入口fail-fast（FND8-11）。redirect流程不会合法走到这里（热
	// 产物define进各代HotModule，冷名字先被genClassMap/classpath装载拦截），命中即
	// 装载器混用；换新实例或加载器仅适用于直接复用本工具类、确需新身份的场景。
	private void checkNotDefined(String className) {
		if (classLoader.isDefined(className))
			throw new IllegalStateException("class already defined in this compiler instance: " + className);
	}

	/**
	 * 批量编译并返回字节码（只在编译器装载器外传递，不在其中define）。
	 * define去向由调用方决定：热模块经RedirectClassSink.defineRedirectClass进各模块
	 * 装载器，冷模块经{@link #defineCompiled}进编译器装载器。
	 */
	public Map<String, byte[]> compileAllToByteCode(Map<String, String> classNameAndCodes) {
		sourceCodes.clear();
		for (Map.Entry<String, String> e : classNameAndCodes.entrySet()) {
			checkNotDefined(e.getKey());
			addSource(e.getKey(), e.getValue());
		}
		String exMsg = compileAll();
		if (exMsg != null)
			throw new IllegalStateException(exMsg);
		var byteCodes = new HashMap<String, byte[]>(classNameAndCodes.size());
		for (String className : classNameAndCodes.keySet())
			byteCodes.put(className, classLoader.getCode(className));
		return byteCodes;
	}

	/**
	 * 在编译器装载器中define并返回compileAllToByteCode的产物。
	 * 冷路径专用（无模块装载器可归属）；热路径产物define进各模块装载器。
	 */
	public Class<?> defineCompiled(String className) throws ClassNotFoundException {
		return classLoader.findClass(className);
	}

	private static final class SourceCode extends SimpleJavaFileObject {
		private final String className;
		private final String contents;

		SourceCode(String className, String contents) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.className = className;
			this.contents = contents;
		}

		public String getClassName() {
			return className;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return contents;
		}
	}

	private static final class CompiledCode extends SimpleJavaFileObject {
		private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		CompiledCode(String className) throws URISyntaxException {
			super(new URI(className), Kind.CLASS);
		}

		@Override
		public OutputStream openOutputStream() {
			return baos;
		}

		byte[] getByteCode() {
			return baos.toByteArray();
		}
	}

	private static final class DynamicClassLoader extends ClassLoader {
		private final HashMap<String, CompiledCode> customCompiledCode = new HashMap<>();

		DynamicClassLoader(ClassLoader parent) {
			super(parent);
		}

		void addCode(CompiledCode cc) {
			customCompiledCode.put(cc.getName(), cc);
		}

		byte[] getCode(String name) {
			CompiledCode cc = customCompiledCode.get(name);
			return cc != null ? cc.getByteCode() : null;
		}

		// 本加载器是否已define该类（findLoadedClass为protected final，嵌套类内封装）
		boolean isDefined(String name) {
			return findLoadedClass(name) != null;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			CompiledCode cc = customCompiledCode.get(name);
			if (cc == null)
				return super.findClass(name);
			byte[] byteCode = cc.getByteCode();
			return defineClass(name, byteCode, 0, byteCode.length);
		}
	}

	private static final class ExtendedJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		private final DynamicClassLoader cl;

		/**
		 * @param fileManager delegate to this file manager
		 */
		ExtendedJavaFileManager(JavaFileManager fileManager, DynamicClassLoader cl) {
			super(fileManager);
			this.cl = cl;
		}

		@Override
		public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
												   JavaFileObject.Kind kind, FileObject sibling) {
			try {
				CompiledCode innerClass = new CompiledCode(className);
				cl.addCode(innerClass);
				return innerClass;
			} catch (Exception e) {
				throw new IllegalStateException("Error while creating in-memory output file for " + className, e);
			}
		}

		@Override
		public ClassLoader getClassLoader(JavaFileManager.Location location) {
			return cl;
		}
	}
}
