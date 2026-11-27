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
import org.jspecify.annotations.Nullable;

/**
 * 内存 Java 源码编译器（单实例非线程安全）。
 * 源自 <a href="https://github.com/trung/InMemoryJavaCompiler">InMemoryJavaCompiler</a>。
 */
public class InMemoryJavaCompiler {
	private final JavaCompiler javac;
	private DynamicClassLoader classLoader;
	private @Nullable Iterable<String> options;
	private final ArrayList<SourceCode> sourceCodes = new ArrayList<>();
	private int ignoreWarningLevel = 2; // 0:>=OTHER 1:>=NOTE 2:>=MANDATORY_WARNING 3:>=WARNING 4:>=ERROR

	public InMemoryJavaCompiler() {
		this(ClassLoader.getSystemClassLoader());
	}

	public InMemoryJavaCompiler(ClassLoader parent) {
		javac = ToolProvider.getSystemJavaCompiler();
		classLoader = new DynamicClassLoader(parent);
	}

	/** 返回编译器内部使用的类加载器。 */
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

	/** 设置编译器参数，例如 {@code -Xlint:unchecked}。 */
	public InMemoryJavaCompiler useOptions(String... options) {
		this.options = Arrays.asList(options);
		return this;
	}

	/** 忽略非致命编译告警（如 unchecked/unsafe 告警）。 */
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
	 * 编译已添加的全部源码。
	 *
	 * @return 失败或告警信息；编译成功返回 null
	 */
	public @Nullable String compileAll() {
		if (sourceCodes.isEmpty())
			return null;
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
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

	// defineCompiled 直接调用 findClass，对已定义类名二次编译会 LinkageError，入口提前拦截。
	private void checkNotDefined(String className) {
		if (classLoader.isDefined(className))
			throw new IllegalStateException("class already defined in this compiler instance: " + className);
	}

	private void compileBatch(Map<String, String> classNameAndCodes) {
		sourceCodes.clear();
		for (Map.Entry<String, String> e : classNameAndCodes.entrySet()) {
			checkNotDefined(e.getKey());
			addSource(e.getKey(), e.getValue());
		}
		// 清除上轮残留字节码，确保本轮未产出的请求类能立即报错
		for (String className : classNameAndCodes.keySet())
			classLoader.customCompiledCode.remove(className);
		String exMsg = compileAll();
		if (exMsg != null)
			throw new IllegalStateException(exMsg);
		for (String className : classNameAndCodes.keySet()) {
			if (!classLoader.hasCode(className))
				throw new IllegalStateException("compiled class not found: " + className);
		}
	}

	/**
	 * 批量编译并在编译器装载器中直接 define 返回已装载的 Class。
	 */
	public Map<String, Class<?>> compileAll(Map<String, String> classNameAndCodes) throws ClassNotFoundException {
		compileBatch(classNameAndCodes);
		var classes = new HashMap<String, Class<?>>(classNameAndCodes.size());
		for (String className : classNameAndCodes.keySet())
			classes.put(className, defineCompiled(className));
		return classes;
	}

	/**
	 * 批量编译并返回字节码（只在编译器装载器外传递，不在其中 define；需要时可由调用方经 {@link #defineCompiled} 定义）。
	 */
	public Map<String, byte[]> compileAllToByteCode(Map<String, String> classNameAndCodes) {
		compileBatch(classNameAndCodes);
		var byteCodes = new HashMap<String, byte[]>(classNameAndCodes.size());
		for (String className : classNameAndCodes.keySet()) {
			var code = classLoader.getCode(className);
			if (code == null)
				throw new IllegalStateException("compiled class not found: " + className);
			byteCodes.put(className, code);
		}
		return byteCodes;
	}

	/**
	 * 在编译器装载器中 define 并返回本实例最近一次批量编译的产物。
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

		boolean hasCode(String name) {
			return customCompiledCode.containsKey(name);
		}

		byte @Nullable [] getCode(String name) {
			CompiledCode cc = customCompiledCode.get(name);
			return cc != null ? cc.getByteCode() : null;
		}

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
