package Zeze.Arch.Gen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllDoneHandle;
import Zeze.Arch.RedirectAllResultHandle;
import Zeze.Arch.RedirectResultHandle;
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
	public java.lang.reflect.Parameter ParameterRedirectResultHandle;
	public java.lang.reflect.Parameter ParameterRedirectAllResultHandle;
	public java.lang.reflect.Parameter ParameterRedirectAllDoneHandle;

	public String getThrows() {
		var throwsexp = method.getGenericExceptionTypes();
		if (throwsexp.length == 0)
			return "";
		var sb = new StringBuilder();
		sb.append(" throws ");
		for (int i = 0; i < throwsexp.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(throwsexp[i].getTypeName());
		}
		return sb.toString();
	}

	public final void PrepareParameters() {
		ParametersAll = method.getParameters();
		ParametersNormal.addAll(Arrays.asList(ParametersAll));

		if (overrideType == overrideType.RedirectToServer || overrideType == overrideType.RedirectHash) {
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
			if (p.getType() == RedirectAllDoneHandle.class)
				ParameterRedirectAllDoneHandle = p;
			else if (p.getType() == RedirectAllResultHandle.class)
				ParameterRedirectAllResultHandle = p;
			else if (p.getType() == RedirectResultHandle.class)
				ParameterRedirectResultHandle = p;
		}
	}

	public final String GetNarmalCallString() throws Throwable {
		return GetNarmalCallString(null);
	}

	public final String GetNarmalCallString(Zeze.Util.Func1<java.lang.reflect.Parameter, Boolean> skip) throws Throwable {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int i = 0; i < ParametersNormal.size(); ++i) {
			var p = ParametersNormal.get(i);
			if (null != skip && skip.call(p)) {
				continue;
			}
			if (first) {
				first = false;
			}
			else {
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
		return Str.format("{}{}", GetHashOrServerCallString(), GetNarmalCallString());
	}

	public final String getRedirectType() {
		switch (overrideType) {
		case RedirectHash: // fall down
			return "ModuleRedirect.RedirectTypeWithHash";

		case RedirectToServer:
			return "ModuleRedirect.RedirectTypeToServer";
		default:
			throw new RuntimeException("unkown OverrideType");
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
		if (overrideType != overrideType.RedirectAll) {
			throw new RuntimeException("is not RedirectAll");
		}
		var attr = (RedirectAll)attribute;
		return attr.GetConcurrentLevelSource();
	}
}
