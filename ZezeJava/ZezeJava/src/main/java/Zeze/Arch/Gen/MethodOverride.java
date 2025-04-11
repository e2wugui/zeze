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
	final String keyHashCode;
	final ArrayList<Parameter> inputParameters = new ArrayList<>();
	final String resultTypeName;
	final ArrayList<Field> resultFields = new ArrayList<>();
	final Type resultType;
	final Class<?> resultClass;
	final boolean oneByone;
	boolean returnTypeHasResultCode;

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
		if (allParameters.length <= 0 || (hashOrServerIdParameter = allParameters[0]).getType() != int.class) {
			throw new IllegalStateException("ModuleRedirect: type of first parameter must be 'int': "
					+ method.getDeclaringClass().getName() + "::" + method.getName());
		}
		oneByone = annotation instanceof RedirectToServer ? ((RedirectToServer)annotation).oneByOne()
				: (annotation instanceof RedirectHash && ((RedirectHash)annotation).oneByOne());
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
					} else
						keyHashCode0 = param.getName() + ".hashCode()";
				} else {
					throw new IllegalStateException("ModuleRedirect: RedirectKey is used more than once: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
			}
		}
		keyHashCode = keyHashCode0 != null ? keyHashCode0 : "Long.hashCode(_t_.getSessionId())";

		inputParameters.addAll(Arrays.asList(allParameters));
		inputParameters.remove(0);

		var rType = method.getGenericReturnType();
		if (rType instanceof ParameterizedType) {
			var rpType = (ParameterizedType)rType;
			if (annotation instanceof RedirectAll) {
				if (rpType.getRawType() == RedirectAllFuture.class) {
					resultType = rpType.getActualTypeArguments()[0];
					resultClass = (Class<?>)(resultType instanceof Class ?
							resultType : ((ParameterizedType)resultType).getRawType());
					if (!RedirectResult.class.isAssignableFrom(resultClass)) {
						throw new IllegalStateException("RedirectAll Result Type Must Extend RedirectResult: "
								+ method.getDeclaringClass().getName() + "::" + method.getName());
					}
				} else {
					resultType = null;
					resultClass = null;
				}
			} else if (rpType.getRawType() == RedirectFuture.class) {
				resultType = rpType.getActualTypeArguments()[0];
				resultClass = (Class<?>)(resultType instanceof Class ?
						resultType : ((ParameterizedType)resultType).getRawType());
				try {
					if (resultClass != Long.class && resultClass != Binary.class)
						resultClass.getConstructor((Class<?>[])null);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("RedirectFuture<> Result Type Must Be 'Long','Binary','String'"
							+ " or any type contains public default constructor: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
			} else {
				resultType = null;
				resultClass = null;
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
			if (Serializable.class.isAssignableFrom(resultClass)) {
				try {
					resultClass.getMethod("setResultCode", long.class);
					returnTypeHasResultCode = true;
				} catch (NoSuchMethodException e) {
					returnTypeHasResultCode = false;
				}
			} else {
				for (var field : resultClass.getFields()) {
					if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) { // 只允许public和可选的volatile
						if (field.getName().equals("resultCode") && field.getType() == long.class)
							returnTypeHasResultCode = true;
						else
							resultFields.add(field);
					}
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
