package notnull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

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
 * @NotNull 方法返回值在 ARETURN 前做 null 检查（IllegalStateException）。
 * 用法: java -javaagent:notnull-agent.jar -cp ...;notnull-agent.jar ...
 */
public class NotNullAgent implements Opcodes {
	private static final String NOT_NULL = "Lorg/jetbrains/annotations/NotNull;";

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
		inst.addTransformer(new NotNullTransformer());
		System.out.println("[NotNullAgent] installed");
	}

	static boolean isProjectClass(String className) {
		for (String p : new String[]{"Zeze/", "Zezex/", "Game/", "ClientGame/", "demo/",
				"UnitTest/", "Benchmark/", "Dbh2/", "MQ/", "Onz/", "Infinite/",
				"TestLog4jQuery/", "RelationalMapping/", "Temp/", "TaskTest/"})
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
			try {
				ClassNode cn = new ClassNode();
				// 保留原始 frames：我们的插入不新增跳转指令、且保持栈高度不变，原 frame 依然有效；
				// 不能用 COMPUTE_FRAMES（重算 frame 需要加载类，会在 transform 里递归触发类定义）
				new ClassReader(classfileBuffer).accept(cn, 0);
				boolean changed = false;
				for (MethodNode mn : cn.methods) {
					if ((mn.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0)
						continue;
					String owner = className.replace('/', '.') + '.' + mn.name;
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
								+ "' of " + owner + " must not be null"));
						ins.add(new MethodInsnNode(INVOKESTATIC, "notnull/NotNullAgent",
								"checkParam", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
						mn.instructions.insert(ins);
						changed = true;
					}
					// 2. @NotNull 返回值检查
					if (Type.getReturnType(mn.desc).getSort() == Type.OBJECT
							&& (hasNotNull(mn.invisibleAnnotations) || hasNotNull(mn.visibleAnnotations))) {
						for (AbstractInsnNode insn : mn.instructions.toArray()) {
							if (insn.getOpcode() != ARETURN)
								continue;
							InsnList ins = new InsnList();
							ins.add(new InsnNode(DUP));
							ins.add(new LdcInsnNode("@NotNull method " + owner + " must not return null"));
							ins.add(new MethodInsnNode(INVOKESTATIC, "notnull/NotNullAgent",
									"checkReturn", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
							mn.instructions.insertBefore(insn, ins);
							changed = true;
						}
					}
				}
				if (!changed)
					return null;
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				cn.accept(cw);
				return cw.toByteArray();
			} catch (Throwable t) {
				System.err.println("[NotNullAgent] skip " + className + ": " + t);
				return null;
			}
		}
	}
}
