package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import Zeze.Util.StringBuilderCs;

public class GenAction {
	public Parameter Parameter;
	public ParameterizedType ParameterizedType;
	public Type[] GenericArguments;

	public GenAction(Parameter p) {
		Parameter = p;
		ParameterizedType = (ParameterizedType)p.getType().getGenericSuperclass();
		//GenericArguments = ParameterizedType.getActualTypeArguments();
	}

	public static GenAction CreateIf(Parameter p) {
		var ParameterType = p.getType();
		var pName = ParameterType.getName();
		if (pName.startsWith("Zeze.Util.Action"))
			return new GenAction(p);
		return null;
	}

	public void PrintGenericInterfaces(StringBuilderCs sb) {
		sb.Append("//");
		//for (var gp : GenericArguments)
		//	sb.Append(gp.getTypeName()).Append(" ");
		sb.AppendLine("");
	}
}
