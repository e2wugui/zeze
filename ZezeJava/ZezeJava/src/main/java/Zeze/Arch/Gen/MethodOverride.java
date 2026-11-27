package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.AnnotatedParameterizedType;
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
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Reflect;
import Zeze.Util.TransactionLevelAnnotation;
import org.jspecify.annotations.Nullable;

/**
 * 一个待重定向方法的元数据：注解、事务级别、参数划分（hash/RedirectKey/输入）与结果类型校验。
 */
final class MethodOverride {
	final Method method;
	final Annotation annotation;
	final TransactionLevel transactionLevel;
	final Parameter[] allParameters;
	final Parameter hashOrServerIdParameter;
	final @Nullable Parameter redirectKeyParameter;
	final String keyHashCode;
	final ArrayList<Parameter> inputParameters = new ArrayList<>();
	final @Nullable String resultTypeName;
	final ArrayList<Field> resultFields = new ArrayList<>();
	final @Nullable Type resultType;
	final @Nullable Class<?> resultClass;
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
			if (!(actualType instanceof Class) && !(actualType instanceof ParameterizedType)) {
				throw new IllegalStateException("ModuleRedirect: redirect future type argument must be a concrete"
						+ " class or parameterized type (no type variables or wildcards): "
						+ method.getDeclaringClass().getName() + "::" + method.getName());
			}
			resultType = actualType;
			resultClass = (Class<?>)(actualType instanceof Class ? actualType : ((ParameterizedType)actualType).getRawType());
			if (returnClass == RedirectAllFuture.class) {
				if (!RedirectResult.class.isAssignableFrom(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Must Extend RedirectResult: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				// RedirectAll 按字段编解码，不支持 Serializable 结果类型。
				if (Gen.isZezeValue(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Can Not Be Serializable: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				if (Gen.isAbstract(resultClass)) {
					throw new IllegalStateException("RedirectAll Result Type Can Not Be Abstract: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
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
			resultTypeName = Gen.instance.getTypeName(((AnnotatedParameterizedType)method.getAnnotatedReturnType())
					.getAnnotatedActualTypeArguments()[0]);
			if (!Gen.isZezeValue(resultClass)) {
				for (var field : resultClass.getFields()) {
					if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) // 只允许public和可选的volatile
						resultFields.add(field);
				}
			}
		}
	}

	String getDefineString() {
		var sb = new StringBuilder();
		var first = true;
		for (var p : allParameters) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(Gen.instance.getTypeName(p.getAnnotatedType())).append(' ').append(p.getName());
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

	@SuppressWarnings("ConstantConditions")
	String getConcurrentLevelSource() {
		var source = ((RedirectHash)annotation).ConcurrentLevelSource();
		return source != null && !source.isBlank() ? source : "0";
	}
}
