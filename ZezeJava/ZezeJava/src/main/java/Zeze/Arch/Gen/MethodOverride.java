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
import java.util.function.Predicate;
import Zeze.Arch.ModuleRedirectAllContext;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Util.Action1;
import Zeze.Util.KV;
import Zeze.Util.Str;

class MethodOverride {
	Method method;
	OverrideType overrideType;
	Annotation attribute;
	Zeze.Transaction.TransactionLevel TransactionLevel;

	Parameter[] allParameters;
	Parameter hashOrServerIdParameter;
	ArrayList<Parameter> inputParameters = new ArrayList<>();
	GenAction resultAction;
	Class<?> resultType;
	String resultTypeName;
	ArrayList<KV<Class<?>, String>> resultTypeNames;
	boolean returnTypeHasResultCode;

	MethodOverride(Zeze.Transaction.TransactionLevel tLevel, Method method, OverrideType type, Annotation attribute) {
		TransactionLevel = tLevel;
		this.method = method;
		overrideType = type;
		this.attribute = attribute;

		allParameters = method.getParameters();
		inputParameters.addAll(Arrays.asList(allParameters));

		if (type == OverrideType.RedirectAll) {
			for (var p : allParameters) {
				if (p.getType() != Action1.class)
					continue;
				var action = new GenAction(p);
				inputParameters.remove(p);
				if (resultAction != null) {
					throw new RuntimeException("Too Many Result Action: "
							+ method.getDeclaringClass().getName() + "::" + method.getName());
				}
				resultAction = action;
				var genType = resultAction.GenericArgument;
				if (genType instanceof ParameterizedType) {
					var paramType = (ParameterizedType)genType;
					if (paramType.getRawType() != ModuleRedirectAllContext.class) {
						throw new RuntimeException("not Action1<ModuleRedirectAllContext<...>>: "
								+ method.getDeclaringClass().getName() + "::" + method.getName());
					}
					resultType = (Class<?>)((ParameterizedType)genType).getActualTypeArguments()[0];
					if (!RedirectResult.class.isAssignableFrom(resultType)) {
						throw new RuntimeException("RedirectAll Result Type Must Extend RedirectContext: "
								+ method.getDeclaringClass().getName() + "::" + method.getName());
					}
				}
			}
		} else {
			hashOrServerIdParameter = allParameters[0];
			if (hashOrServerIdParameter.getType() != int.class) {
				throw new RuntimeException("ModuleRedirect: type of first parameter must be 'int': "
						+ method.getDeclaringClass().getName() + "::" + method.getName());
			}
			inputParameters.remove(0);
			var rType = method.getGenericReturnType();
			if (rType instanceof ParameterizedType) {
				var rpType = (ParameterizedType)rType;
				if (rpType.getRawType() == RedirectFuture.class) {
					resultType = (Class<?>)rpType.getActualTypeArguments()[0];
				}
			}
		}

		if (resultType == null)
			resultType = Long.class;
		resultTypeName = GenAction.toShortTypeName(resultType.getName()).replace('$', '.');
		Field[] fields = resultType.getFields();
		Arrays.sort(fields, Comparator.comparing(Field::getName));
		resultTypeNames = new ArrayList<>();
		for (Field field : fields) {
			if ((field.getModifiers() & ~Modifier.VOLATILE) == Modifier.PUBLIC) {
				if (field.getName().equals("resultCode") && field.getType() == long.class)
					returnTypeHasResultCode = true;
				else
					resultTypeNames.add(KV.Create(field.getType(), field.getName()));
			}
		}
	}

	String GetDefineString() throws Throwable {
		var sb = new Zeze.Util.StringBuilderCs();
		boolean first = true;
		for (var p : allParameters) {
			if (first)
				first = false;
			else
				sb.Append(", ");
			if (null != resultAction && p == resultAction.Parameter)
				sb.Append(resultAction.GetDefineName());
			else
				sb.Append(Gen.Instance.GetTypeName(p.getType()));
			sb.Append(" ");
			sb.Append(p.getName());
		}
		return sb.toString();
	}

	final String GetNormalCallString() {
		return GetNormalCallString(null);
	}

	final String GetNormalCallString(Predicate<Parameter> skip) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Parameter p : inputParameters) {
			if (null != skip && skip.test(p)) {
				continue;
			}
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(p.getName());
		}
		return sb.toString();
	}

	final String GetHashOrServerCallString() {
		if (hashOrServerIdParameter == null) {
			return "";
		}
		if (allParameters.length == 1) { // 除了hash，没有其他参数。
			return hashOrServerIdParameter.getName();
		}
		return Str.format("{}, ", hashOrServerIdParameter.getName());
	}

	final String GetBaseCallString() {
		return Str.format("{}{}", GetHashOrServerCallString(), GetNormalCallString());
	}

	final String getRedirectType() {
		switch (overrideType) {
		case RedirectHash: // fall down
			return "Zeze.Beans.ProviderDirect.ModuleRedirect.RedirectTypeWithHash";
		case RedirectToServer:
			return "Zeze.Beans.ProviderDirect.ModuleRedirect.RedirectTypeToServer";
		default:
			throw new RuntimeException("unknown OverrideType");
		}
	}

	final String GetChoiceHashOrServerCodeSource() {
		switch (overrideType) {
		case RedirectToServer:
		case RedirectHash:
			return hashOrServerIdParameter.getName(); // parameter name

		default:
			throw new RuntimeException("error state");
		}
	}

	final String GetConcurrentLevelSource() {
		if (overrideType != OverrideType.RedirectAll) {
			throw new RuntimeException("is not RedirectAll");
		}
		var attr = (RedirectAll)attribute;
		return attr.GetConcurrentLevelSource();
	}
}
