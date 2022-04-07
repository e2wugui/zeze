package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllDoneHandle;
import Zeze.Util.Str;

public class MethodOverride {
	public java.lang.reflect.Method method;
	public OverrideType overrideType;
	public Annotation attribute;
	public Zeze.Transaction.TransactionLevel TransactionLevel;

	public MethodOverride(Zeze.Transaction.TransactionLevel tLevel,
						  java.lang.reflect.Method method, OverrideType type, Annotation attribute) {
		TransactionLevel = tLevel;
		this.method = method;
		overrideType = type;
		this.attribute = attribute;
	}

	public java.lang.reflect.Parameter ParameterHashOrServer;
	public ArrayList<Parameter> ParametersNormal = new ArrayList<> ();
	public java.lang.reflect.Parameter[] ParametersAll;
	public GenAction ResultHandle;
	public java.lang.reflect.Parameter ParameterRedirectAllDoneHandle;

	public String getThrows() {
		var throwTypes = method.getGenericExceptionTypes();
		if (throwTypes.length == 0)
			return "";
		var sb = new StringBuilder();
		sb.append(" throws ");
		for (int i = 0; i < throwTypes.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(throwTypes[i].getTypeName());
		}
		return sb.toString();
	}

	public final void PrepareParameters() {
		ParametersAll = method.getParameters();
		ParametersNormal.addAll(Arrays.asList(ParametersAll));

		if (overrideType == OverrideType.RedirectToServer || overrideType == OverrideType.RedirectHash) {
			ParameterHashOrServer = ParametersAll[0];
			if (ParameterHashOrServer.getType() != int.class) {
				throw new RuntimeException("ModuleRedirectWithHash: type of first parameter must be 'int'");
			}
			//System.out.println(ParameterFirstWithHash.getName() + "<-----");
			//if (false == ParameterFirstWithHash.getName().equals("hash")) {
			//	throw new RuntimeException("ModuleRedirectWithHash: name of first parameter must be 'hash'");
			//}
			ParametersNormal.remove(0);
		}

		for (var p : ParametersAll) {
			var handle = GenAction.CreateIf(p);
			if (ResultHandle != null && handle != null)
				throw new RuntimeException("Too Many Result Handle. " + method.getDeclaringClass().getName() + "::" + method.getName());
			if (handle != null)
				ResultHandle = handle;

			if (p.getType() == RedirectAllDoneHandle.class)
				ParameterRedirectAllDoneHandle = p; // 需要传递给处理流程。
		}
	}

	public String GetDefineString() throws Throwable {
		var sb = new Zeze.Util.StringBuilderCs();
		boolean first = true;
		for (var p : ParametersAll) {
			if (first)
				first = false;
			else
				sb.Append(", ");
			if (null != ResultHandle && p == ResultHandle.Parameter)
				sb.Append(ResultHandle.GetDefineName());
			else
				sb.Append(Gen.Instance.GetTypeName(p.getType()));
			sb.Append(" ");
			sb.Append(p.getName());
		}
		return sb.toString();
	}

	public final String GetNormalCallString() throws Throwable {
		return GetNormalCallString(null);
	}

	public final String GetNormalCallString(Zeze.Util.Func1<java.lang.reflect.Parameter, Boolean> skip) throws Throwable {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Parameter p : ParametersNormal) {
			if (null != skip && skip.call(p)) {
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

	public final String GetHashOrServerCallString() {
		if (ParameterHashOrServer == null) {
			return "";
		}
		if (ParametersAll.length == 1) { // 除了hash，没有其他参数。
			return ParameterHashOrServer.getName();
		}
		return Str.format("{}, ", ParameterHashOrServer.getName());
	}

	public final String GetBaseCallString() throws Throwable {
		return Str.format("{}{}", GetHashOrServerCallString(), GetNormalCallString());
	}

	public final String getRedirectType() {
		switch (overrideType) {
		case RedirectHash: // fall down
			return "ModuleRedirect.RedirectTypeWithHash";

		case RedirectToServer:
			return "ModuleRedirect.RedirectTypeToServer";
		default:
			throw new RuntimeException("unknown OverrideType");
		}
	}

	public final String GetChoiceHashOrServerCodeSource() {
		switch (overrideType) {
		case RedirectToServer:
		case RedirectHash:
			return ParameterHashOrServer.getName(); // parameter name

		default:
			throw new RuntimeException("error state");
		}
	}

	public final String GetConcurrentLevelSource() {
		if (overrideType != OverrideType.RedirectAll) {
			throw new RuntimeException("is not RedirectAll");
		}
		var attr = (RedirectAll)attribute;
		return attr.GetConcurrentLevelSource();
	}
}
