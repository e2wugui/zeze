package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import Zeze.Arch.RedirectResult;
import Zeze.Util.StringBuilderCs;

public class GenAction {
	public Parameter Parameter;
	public String Name;
	public ParameterizedType ParameterizedType;
	public Type[] GenericArguments;

	public GenAction(Parameter p) {
		Parameter = p;
		Name = p.getType().getName();
		ParameterizedType = (ParameterizedType)p.getParameterizedType();
		GenericArguments = ParameterizedType.getActualTypeArguments();
	}

	public static GenAction CreateIf(Parameter p) {
		var ParameterType = p.getType();
		var pName = ParameterType.getName();
		if (pName.startsWith("Zeze.Util.Action"))
			return new GenAction(p);
		return null;
	}

	public void GenDecodeAndCallback(String prefix, StringBuilderCs sb, MethodOverride m, String bb) throws Throwable {
		GenDecodeAndCallback("App.Zeze", prefix, sb, m.ResultHandle.Parameter.getName(), m, bb);
	}

	public void GenDecodeAndCallback(String zzName, String prefix, StringBuilderCs sb, String actName, MethodOverride m, String bb) throws Throwable {
		var resultVarNames = new ArrayList<String>();
		for (int i = 0; i < m.ResultHandle.GenericArguments.length; ++i) {
			resultVarNames.add("tmp" + Gen.Instance.TmpVarNameId.incrementAndGet());
			var rClass = (Class<?>)m.ResultHandle.GenericArguments[i];
			Gen.Instance.GenLocalVariable(sb, prefix, rClass, resultVarNames.get(i));
			Gen.Instance.GenDecode(sb, prefix, bb, rClass, resultVarNames.get(i));
		}
		switch (m.TransactionLevel) {
		case Serializable:
		case AllowDirtyWhenAllRead:
			sb.AppendLine("{}{}.NewProcedure(() -> { {}.run({}); return 0L; }, \"ModuleRedirectResponse Procedure\").Call();",
					prefix, zzName, actName, GetCallString(resultVarNames));
			break;

		default:
			sb.AppendLine("{}{}.run({});", prefix, actName, GetCallString(resultVarNames));
			break;
		}
	}

	public void GenEncode(List<String> resultVarNames, String prefix, StringBuilderCs sb, MethodOverride m, String bb) throws Throwable {
		for (int i = 0; i < m.ResultHandle.GenericArguments.length; ++i) {
			var rClass = (Class<?>)m.ResultHandle.GenericArguments[i];
			Gen.Instance.GenEncode(sb, prefix, bb, rClass, resultVarNames.get(i));
		}
	}

	public String GetCallString(List<String> vars) {
		var sb = new StringBuilder();
		for (int i = 0; i < vars.size(); ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(vars.get(i));
		}
		return sb.toString();
	}

	private static String toShortTypeName(String typeName) {
		if (!typeName.startsWith("java.lang."))
			return typeName;
		String shortTypeName = typeName.substring(10);
		return shortTypeName.indexOf('.') < 0 ? shortTypeName : typeName;
	}

	public String GetDefineName() {
		var sb = new StringBuilder();
		sb.append(Name).append("<");
		for (int i = 0; i < GenericArguments.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(toShortTypeName(GenericArguments[i].getTypeName().replace('$', '.')));
		}
		sb.append(">");
		return sb.toString();
	}

	public void Verify(MethodOverride m) {
		switch (m.overrideType) {
		case RedirectHash:
		case RedirectToServer:
			break;

		case RedirectAll:
			if (GenericArguments.length != 1)
				throw new RuntimeException(m.method.getName() + ": RedirectAll Result Handle Too Many Parameters.");
			if (!RedirectResult.class.isAssignableFrom(m.ResultType))
				throw new RuntimeException(m.method.getName() + ": RedirectAll Result Type Must Extend RedirectContext");
			break;
		}
	}
}
