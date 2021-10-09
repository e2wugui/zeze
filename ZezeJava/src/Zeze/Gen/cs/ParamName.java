package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class ParamName extends TypeName {
	public static String GetParamList(Collection<Types.Variable> variables) {
		StringBuilder plist = new StringBuilder();
		boolean first = true;
		for (Types.Variable var : variables) {
			if (first) {
				first = false;
			}
			else {
				plist.append(", ");
			}
			plist.append(ParamName.GetName(var.getVariableType())).append(" _").append(var.getName()).append("_");
		}
		return plist.toString();
	}
}