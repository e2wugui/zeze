package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectKey;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Net.Binary;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Reflect;
import Zeze.Util.TransactionLevelAnnotation;

final class MethodOverride {
	final Method method;
	final Annotation annotation;
	final TransactionLevel transactionLevel;
	final Parameter[] allParameters;
	final Parameter hashOrServerIdParameter;
	final Parameter redirectKeyParameter;
	final String keyHashCode;
	final ArrayList<Parameter> inputParameters = new ArrayList<>();
	final String resultTypeName;
	final ArrayList<Field> resultFields = new ArrayList<>();
	final Type resultType;
	final Class<?> resultClass;
	final boolean oneByOne;

	MethodOverride(Method method, Annotation annotation) {
		this.method = method;
		this.annotation = annotation;

		if ((method.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0)
			throw new IllegalStateException("ModuleRedirect: method can not be static or final: " +
					method.getDeclaringClass().getName() + '.' + method.getName());

		if ((method.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0)
			throw new IllegalStateException("ModuleRedirect: method must be public or protected: " +
					method.getDeclaringClass().getName() + '.' + method.getName());

		if ((method.getDeclaringClass().getModifiers() & Modifier.FINAL) != 0)
			throw new IllegalStateException("ModuleRedirect: class can not be final: " +
					method.getDeclaringClass().getName());

		if ((method.getDeclaringClass().getModifiers() & Modifier.PUBLIC) == 0)
			throw new IllegalStateException("ModuleRedirect: class must be public: " +
					method.getDeclaringClass().getName());

		var levelAnn = method.getAnnotation(TransactionLevelAnnotation.class);
		transactionLevel = levelAnn != null ? levelAnn.Level() : TransactionLevel.Serializable;

		allParameters = method.getParameters();
		if (allParameters.length == 0 || (hashOrServerIdParameter = allParameters[0]).getType() != int.class) {
			throw new IllegalStateException("ModuleRedirect: type of first parameter must be 'int': "
					+ method.getDeclaringClass().getName() + "::" + method.getName());
		}
		oneByOne = annotation instanceof RedirectToServer ? ((RedirectToServer)annotation).oneByOne()
				: (annotation instanceof RedirectHash && ((RedirectHash)annotation).oneByOne());
		Parameter redirectKeyParameter0 = null;
		String keyHashCode0 = null;
		for (var param : allParameters) {
			var keyAnn = param.getAnnotation(RedirectKey.class);
			if (keyAnn != null) {
				if (annotation instanceof RedirectAll) {
					throw new IllegalStateException("ModuleRedirect: RedirectKey can not be used for RedirectAll: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				if (keyHashCode0 == null) {
					var paramType = param.getType();
					if (paramType.isPrimitive()) {
						if (paramType == int.class)
							keyHashCode0 = param.getName();
						else {
							var boxType = Reflect.getBoxClass(paramType);
							if (boxType == null) {
								throw new IllegalStateException("ModuleRedirect: unknown primitive type: "
										+ paramType.getName() + " in " + method.getDeclaringClass().getName()
										+ "::" + method.getName());
							}
							keyHashCode0 = boxType.getSimpleName() + ".hashCode(" + param.getName() + ')';
						}
					} else {
						// 数组是身份哈希：内容相同的key会落入不同oneByOne串行队列，串行语义静默破坏
						// 且跨重启漂移。生成期fail-fast，提示改用Binary（内容哈希）。
						if (paramType.isArray()) {
							throw new IllegalStateException("ModuleRedirect: RedirectKey can not be used on array type"
									+ " (use Zeze.Net.Binary for content-based hashing): "
									+ method.getDeclaringClass().getName() + "::" + method.getName());
						}
						keyHashCode0 = param.getName() + ".hashCode()";
					}
					redirectKeyParameter0 = param;
				} else {
					throw new IllegalStateException("ModuleRedirect: RedirectKey is used more than once: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
			}
		}
		redirectKeyParameter = redirectKeyParameter0;
		keyHashCode = keyHashCode0 != null ? keyHashCode0 : "Long.hashCode(_t_.getSessionId())";

		inputParameters.addAll(Arrays.asList(allParameters));
		inputParameters.removeFirst();

		// AG1-F2：redirect返回类型校验收口。此前三个缺口在模块创建期以晦涩方式崩溃：
		// a) All配RedirectFuture（或Hash/ToServer配RedirectAllFuture）落错分支，resultType=null
		//    时生成非法源码RedirectFuture<null>；b) raw泛型返回（不带<...>）时getGenericReturnType
		//    就是Class本身，同样落空得到null实参；c) TypeVariable/通配符实参在下面被盲转成
		//    ParameterizedType直接CCE。这里统一fail-fast给出带方法名的清晰错误。
		var returnClass = method.getReturnType();
		var rType = method.getGenericReturnType();
		if (returnClass == RedirectFuture.class || returnClass == RedirectAllFuture.class) {
			if (returnClass == RedirectAllFuture.class != (annotation instanceof RedirectAll)) {
				throw new IllegalStateException("ModuleRedirect: RedirectAll must be paired with RedirectAllFuture"
						+ " and RedirectHash/RedirectToServer must be paired with RedirectFuture: "
						+ method.getDeclaringClass().getName() + "::" + method.getName());
			}
			if (!(rType instanceof ParameterizedType rpType)) {
				throw new IllegalStateException("ModuleRedirect: redirect future must not be raw type"
						+ " (declare concrete type arguments): "
						+ method.getDeclaringClass().getName() + "::" + method.getName());
			}
			var actualType = rpType.getActualTypeArguments()[0];
			// 实参仅允许Class/ParameterizedType：TypeVariable/通配符无法在生成代码中命名。
			if (!(actualType instanceof Class) && !(actualType instanceof ParameterizedType)) {
				throw new IllegalStateException("ModuleRedirect: redirect future type argument must be a concrete"
						+ " class or parameterized type (no type variables or wildcards): "
						+ method.getDeclaringClass().getName() + "::" + method.getName());
			}
			resultType = actualType;
			// 实参种类已守卫，此处转换必然安全。
			resultClass = (Class<?>)(actualType instanceof Class ? actualType : ((ParameterizedType)actualType).getRawType());
			if (returnClass == RedirectAllFuture.class) {
				if (!RedirectResult.class.isAssignableFrom(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Must Extend RedirectResult: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				// FND2-A1-1：All路径的生成代码对Serializable结果不收集字段（resultFields为空），
				// 接收端不编码、发起端不解码，分组结果全是空对象且无任何诊断；ToServer/Hash路径
				// 支持Serializable，All独缺该分支。fail-fast拒绝该组合，对齐上面的签名硬校验。
				if (Serializable.class.isAssignableFrom(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Can Not Be Serializable: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				// FND8-83：生成代码new结果类实例，抽象类生成源码不可编译，fail-fast拒绝。
				if (Gen.isAbstract(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Can Not Be Abstract: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				// FND8-85：对齐RedirectFuture分支，同样要求public默认构造器。
				try {
					resultClass.getConstructor((Class<?>[])null);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("RedirectAll Result Type Must Be 'Long','Binary','String'"
							+ " or any type contains public default constructor: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
			} else {
				try {
					if (resultClass != Long.class && resultClass != Binary.class)
						resultClass.getConstructor((Class<?>[])null);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("RedirectFuture<> Result Type Must Be 'Long','Binary','String'"
							+ " or any type contains public default constructor: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				// FND8-83：同上，抽象结果类生成源码不可编译。
				if (resultClass != Long.class && resultClass != Binary.class && Gen.isAbstract(resultClass)) {
					throw new IllegalStateException("RedirectFuture<> Result Type Can Not Be Abstract: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
			}
		} else {
			resultType = null;
			resultClass = null;
		}

		if (resultType == null)
			resultTypeName = null;
		else {
			resultTypeName = toShort((resultType == resultClass ?
					resultClass.getName() : resultType.toString()).replace('$', '.'));
			if (!Serializable.class.isAssignableFrom(resultClass)) {
				for (var field : resultClass.getFields()) {
					if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) // 只允许public和可选的volatile
						resultFields.add(field);
				}
			}
		}
	}

	private static String toShort(String typeName) {
		return typeName.startsWith("java.lang.") && typeName.indexOf('.', 10) < 0 ? typeName.substring(10) : typeName;
	}

	String getDefineString() {
		var sb = new StringBuilder();
		var first = true;
		for (var p : allParameters) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(Gen.instance.getTypeName(p.getParameterizedType())).append(' ').append(p.getName());
		}
		return sb.toString();
	}

	String getNormalCallString() {
		var sb = new StringBuilder();
		var first = true;
		for (var p : inputParameters) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(p.getName());
		}
		return sb.toString();
	}

	String getBaseCallString() {
		return inputParameters.isEmpty()
				? hashOrServerIdParameter.getName() // 除了serverId或hash,没有其他参数
				: hashOrServerIdParameter.getName() + ", " + getNormalCallString();
	}

	String getRedirectType() {
		return annotation instanceof RedirectToServer
				? "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer"
				: "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash";
	}

	String getConcurrentLevelSource() {
		var source = ((RedirectHash)annotation).ConcurrentLevelSource();
		return source != null && !source.isBlank() ? source : "0";
	}
}
