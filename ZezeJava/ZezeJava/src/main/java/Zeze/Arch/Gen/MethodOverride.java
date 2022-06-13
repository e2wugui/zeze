package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.KV;
import Zeze.Util.TransactionLevelAnnotation;

final class MethodOverride {
	final Method method;
	final Annotation annotation;
	final TransactionLevel transactionLevel;
	final Parameter[] allParameters;
	final Parameter hashOrServerIdParameter;
	final ArrayList<Parameter> inputParameters = new ArrayList<>();
	final String resultTypeName;
	final ArrayList<KV<Class<?>, String>> resultTypeNames = new ArrayList<>();
	Class<?> resultType;
	boolean returnTypeHasResultCode;

	MethodOverride(Method method, Annotation annotation) {
		this.method = method;
		this.annotation = annotation;

		var levelAnn = method.getAnnotation(TransactionLevelAnnotation.class);
		transactionLevel = levelAnn != null ? levelAnn.Level() : TransactionLevel.Serializable;

		allParameters = method.getParameters();
		inputParameters.addAll(Arrays.asList(allParameters));
		hashOrServerIdParameter = allParameters[0];
		if (hashOrServerIdParameter.getType() != int.class) {
			throw new RuntimeException("ModuleRedirect: type of first parameter must be 'int': "
					+ method.getDeclaringClass().getName() + "::" + method.getName());
		}
		inputParameters.remove(0);

		var rType = method.getGenericReturnType();
		if (rType instanceof ParameterizedType) {
			var rpType = (ParameterizedType)rType;
			if (annotation instanceof RedirectAll) {
				if (rpType.getRawType() == RedirectAllFuture.class) {
					resultType = (Class<?>)rpType.getActualTypeArguments()[0];
					if (!RedirectResult.class.isAssignableFrom(resultType)) {
						throw new RuntimeException("RedirectAll Result Type Must Extend RedirectResult: "
								+ method.getDeclaringClass().getName() + "::" + method.getName());
					}
				}
			} else if (rpType.getRawType() == RedirectFuture.class)
				resultType = (Class<?>)rpType.getActualTypeArguments()[0];
		}

		if (resultType == null) {
			if (annotation instanceof RedirectAll) {
				resultTypeName = null;
				return;
			}
			resultType = Long.class;
		}
		resultTypeName = toShort(resultType.getName()).replace('$', '.');
		for (var field : resultType.getFields()) {
			if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) { // 只允许public和可选的volatile
				if (field.getName().equals("resultCode") && field.getType() == long.class)
					returnTypeHasResultCode = true;
				else
					resultTypeNames.add(KV.Create(field.getType(), field.getName()));
			}
		}
	}

	private static String toShort(String typeName) {
		return typeName.startsWith("java.lang.") && typeName.indexOf('.', 10) < 0 ? typeName.substring(10) : typeName;
	}

	String GetDefineString() {
		var sb = new StringBuilder();
		var first = true;
		for (var p : allParameters) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(Gen.Instance.GetTypeName(p.getType())).append(' ').append(p.getName());
		}
		return sb.toString();
	}

	String GetNormalCallString() {
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

	String GetBaseCallString() {
		return inputParameters.isEmpty()
				? hashOrServerIdParameter.getName() // 除了serverId或hash,没有其他参数
				: hashOrServerIdParameter.getName() + ", " + GetNormalCallString();
	}

	String getRedirectType() {
		return annotation instanceof RedirectToServer
				? "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer"
				: "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash";
	}

	String getConcurrentLevelSource() {
		var source = ((RedirectHash)annotation).ConcurrentLevelSource();
		return source != null && !source.isBlank() ? source : "1";
	}
}
