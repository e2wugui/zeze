package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class GenAction {
	Parameter Parameter;
	Type GenericArgument;

	GenAction(Parameter p) {
		Parameter = p;
		GenericArgument = ((ParameterizedType)p.getParameterizedType()).getActualTypeArguments()[0];
	}

	static String toShortTypeName(String typeName) {
		if (!typeName.startsWith("java.lang."))
			return typeName;
		String shortTypeName = typeName.substring(10);
		return shortTypeName.indexOf('.') < 0 ? shortTypeName : typeName;
	}

	String GetDefineName() {
		return "Zeze.Util.Action1<" + toShortTypeName(GenericArgument.getTypeName()).replace('$', '.') + '>';
	}
}
