package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Util.KV;
import Zeze.Util.StringBuilderCs;

class MethodOverride {
	final Method method;
	final Annotation annotation;
	final Zeze.Transaction.TransactionLevel TransactionLevel;
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

		var levelAnn = method.getAnnotation(Zeze.Util.TransactionLevel.class);
		TransactionLevel = levelAnn != null
				? Zeze.Transaction.TransactionLevel.valueOf(levelAnn.Level())
				: Zeze.Transaction.TransactionLevel.Serializable;

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

		if (resultType == null)
			resultType = Long.class;
		resultTypeName = toShort(resultType.getName()).replace('$', '.');
		var fields = resultType.getFields();
		Arrays.sort(fields, Comparator.comparing(Field::getName));
		for (var field : fields) {
			if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) { // 只允许public和可选的volatile
				if (field.getName().equals("resultCode") && field.getType() == long.class)
					returnTypeHasResultCode = true;
				else
					resultTypeNames.add(KV.Create(field.getType(), field.getName()));
			}
		}
	}

	private static String toShort(String typeName) {
		if (!typeName.startsWith("java.lang."))
			return typeName;
		String shortTypeName = typeName.substring(10);
		return shortTypeName.indexOf('.') < 0 ? shortTypeName : typeName;
	}

	String GetDefineString() throws Throwable {
		var sb = new StringBuilderCs();
		var first = true;
		for (var p : allParameters) {
			if (!first)
				sb.Append(", ");
			first = false;
			sb.Append(Gen.Instance.GetTypeName(p.getType())).Append(' ').Append(p.getName());
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
				? "Zeze.Beans.ProviderDirect.ModuleRedirect.RedirectTypeToServer"
				: "Zeze.Beans.ProviderDirect.ModuleRedirect.RedirectTypeWithHash";
	}
}
