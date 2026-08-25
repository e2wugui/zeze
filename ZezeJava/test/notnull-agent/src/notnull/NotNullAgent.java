package notnull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 模拟 IDEA "Add runtime assertions for notnull-annotated methods" 的 javaagent。
 * 对项目包内所有方法：@NotNull 参数在方法入口做 null 检查（IllegalArgumentException），
 * @NotNull 方法返回值（含数组返回）在 ARETURN 前做 null 检查（IllegalStateException），文案与 IDEA 一致。
 * 用法: java -javaagent:notnull-agent.jar[=包前缀1,包前缀2,...] -cp ...;notnull-agent.jar ...
 * 不带参数时使用 DEFAULT_PREFIXES；JVM 退出时向 stderr 汇总插桩统计，失败的类逐个列出。
 */
public class NotNullAgent implements Opcodes {
	private static final String NOT_NULL = "Lorg/jetbrains/annotations/NotNull;";

	// 默认插桩的包前缀（ZezeJava 仓库内的 java 包根），可用 agent 参数覆盖，见 parsePrefixes。
	private static final String[] DEFAULT_PREFIXES = new String[]{
			"Zeze/", "Zezex/", "Game/", "ClientGame/", "demo/",
			"UnitTest/", "Benchmark/", "Dbh2/", "MQ/", "Onz/", "Infinite/",
			"TestLog4jQuery/", "RelationalMapping/", "Temp/", "TaskTest/",
			"GlobalRaft/", "SimpleRaft/"};

	private static volatile String[] prefixes = DEFAULT_PREFIXES;

	// transform 失败只能跳过（fail-open），退出时必须汇总报告，避免静默漏插桩掩盖覆盖缺口。
	private static final AtomicLong matchedClasses = new AtomicLong();
	private static final AtomicLong instrumentedClasses = new AtomicLong();
	private static final AtomicLong unchangedClasses = new AtomicLong();
	private static final AtomicLong paramChecks = new AtomicLong();
	private static final AtomicLong returnChecks = new AtomicLong();
	private static final ConcurrentLinkedQueue<String> failedClasses = new ConcurrentLinkedQueue<>();

	// 与 IDEA 报错信息保持一致
	public static void checkParam(Object value, String msg) {
		if (value == null)
			throw new IllegalArgumentException(msg);
	}

	public static void checkReturn(Object value, String msg) {
		if (value == null)
			throw new IllegalStateException(msg);
	}

	public static void premain(String args, Instrumentation inst) {
		prefixes = parsePrefixes(args);
		inst.addTransformer(new NotNullTransformer());
		System.out.println("[NotNullAgent] installed, prefixes=" + String.join(",", prefixes));
		Runtime.getRuntime().addShutdownHook(new Thread(NotNullAgent::report, "NotNullAgent-report"));
	}

	/**
	 * agent 参数为逗号分隔的包前缀，尾部 '/' 可省略（自动补上）；
	 * 省略、为空或全是空项时回退到 DEFAULT_PREFIXES。
	 */
	static String[] parsePrefixes(String args) {
		if (args == null || args.isBlank())
			return DEFAULT_PREFIXES;
		var list = new ArrayList<String>();
		for (var p : args.split(",")) {
			p = p.trim();
			if (!p.isEmpty())
				list.add(p.endsWith("/") ? p : p + "/");
		}
		if (list.isEmpty()) {
			System.err.println("[NotNullAgent] agent 参数没有有效前缀，回退到默认包前缀");
			return DEFAULT_PREFIXES;
		}
		return list.toArray(new String[0]);
	}

	private static void report() {
		System.err.println("[NotNullAgent] classes matched=" + matchedClasses.get()
				+ " instrumented=" + instrumentedClasses.get()
				+ " unchanged=" + unchangedClasses.get()
				+ " failed=" + failedClasses.size()
				+ ", checks param=" + paramChecks.get()
				+ " return=" + returnChecks.get());
		for (var c : failedClasses)
			System.err.println("[NotNullAgent] transform failed: " + c);
	}

	static boolean isProjectClass(String className) {
		for (var p : prefixes)
			if (className.startsWith(p))
				return true;
		return false;
	}

	private static boolean hasNotNull(List<AnnotationNode> anns) {
		if (anns != null)
			for (AnnotationNode an : anns)
				if (NOT_NULL.equals(an.desc))
					return true;
		return false;
	}

	static class NotNullTransformer implements ClassFileTransformer {
		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
								ProtectionDomain protectionDomain, byte[] classfileBuffer) {
			if (className == null || !isProjectClass(className))
				return null;
			matchedClasses.incrementAndGet();
			try {
				ClassNode cn = new ClassNode();
				// 保留原始 frames：我们的插入不新增跳转指令、且保持栈高度不变，原 frame 依然有效；
				// 不能用 COMPUTE_FRAMES（重算 frame 需要加载类，会在 transform 里递归触发类定义）
				new ClassReader(classfileBuffer).accept(cn, 0);
				boolean changed = false;
				for (MethodNode mn : cn.methods) {
					if ((mn.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0)
						continue;
					// 报错文案里定位方法用的完整标识：类名.方法名
					String methodId = className.replace('/', '.') + '.' + mn.name;
					// 1. @NotNull 参数入口检查
					Type[] argTypes = Type.getArgumentTypes(mn.desc);
					for (int i = 0; i < argTypes.length; i++) {
						int sort = argTypes[i].getSort();
						if (sort != Type.OBJECT && sort != Type.ARRAY)
							continue;
						List<AnnotationNode> inv = mn.invisibleParameterAnnotations == null
								|| i >= mn.invisibleParameterAnnotations.length
								? null : mn.invisibleParameterAnnotations[i];
						List<AnnotationNode> vis = mn.visibleParameterAnnotations == null
								|| i >= mn.visibleParameterAnnotations.length
								? null : mn.visibleParameterAnnotations[i];
						if (!hasNotNull(inv) && !hasNotNull(vis))
							continue;
						int slot = (mn.access & ACC_STATIC) != 0 ? 0 : 1;
						for (int j = 0; j < i; j++)
							slot += argTypes[j].getSize();
						String pname = "arg" + i;
						if (mn.parameters != null && i < mn.parameters.size()
								&& ((ParameterNode)mn.parameters.get(i)).name != null)
							pname = ((ParameterNode)mn.parameters.get(i)).name;
						InsnList ins = new InsnList();
						ins.add(new VarInsnNode(ALOAD, slot));
						ins.add(new LdcInsnNode("Argument for @NotNull parameter '" + pname
								+ "' of " + methodId + " must not be null"));
						ins.add(new MethodInsnNode(INVOKESTATIC, "notnull/NotNullAgent",
								"checkParam", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
						mn.instructions.insert(ins);
						paramChecks.incrementAndGet();
						changed = true;
					}
					// 2. @NotNull 返回值检查（对象与数组返回都走 ARETURN）
					int returnSort = Type.getReturnType(mn.desc).getSort();
					if ((returnSort == Type.OBJECT || returnSort == Type.ARRAY)
							&& (hasNotNull(mn.invisibleAnnotations) || hasNotNull(mn.visibleAnnotations))) {
						for (AbstractInsnNode insn : mn.instructions.toArray()) {
							if (insn.getOpcode() != ARETURN)
								continue;
							InsnList ins = new InsnList();
							ins.add(new InsnNode(DUP));
							ins.add(new LdcInsnNode("@NotNull method " + methodId + " must not return null"));
							ins.add(new MethodInsnNode(INVOKESTATIC, "notnull/NotNullAgent",
									"checkReturn", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
							mn.instructions.insertBefore(insn, ins);
							returnChecks.incrementAndGet();
							changed = true;
						}
					}
				}
				if (!changed) {
					unchangedClasses.incrementAndGet();
					return null;
				}
				instrumentedClasses.incrementAndGet();
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				cn.accept(cw);
				return cw.toByteArray();
			} catch (Throwable t) {
				failedClasses.add(className);
				System.err.println("[NotNullAgent] skip " + className + ": " + t);
				return null;
			}
		}
	}
}
