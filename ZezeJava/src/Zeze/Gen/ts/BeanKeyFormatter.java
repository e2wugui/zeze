package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;
import java.io.*;

public class BeanKeyFormatter {
	private Types.BeanKey beanKey;

	public BeanKeyFormatter(Types.BeanKey beanKey) {
		this.beanKey = beanKey;
	}

	public static String GetParamListWithDefault(Collection<Types.Variable> variables) {
		StringBuilder plist = new StringBuilder();
		boolean first = true;
		for (Types.Variable var : variables) {
			if (first) {
				first = false;
			}
			else {
				plist.append(", ");
			}
			plist.append(var.getNamePrivate()).append("_: ").append(TypeName.GetName(var.getVariableType())).append(" = ").append(Default.GetDefault(var));
		}
		return plist.toString();
	}

	public final void Make(OutputStreamWriter sw) {
		sw.write("export class " + beanKey.getSpace().Path("_", beanKey.getName()) + " implements Zeze.Bean {" + System.lineSeparator());
		// declare enums
		for (Types.Enum e : beanKey.getEnums()) {
			sw.write("    public static readonly " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
		}
		if (!beanKey.getEnums().isEmpty()) {
			sw.write("" + System.lineSeparator());
		}

		// declare variables
		for (Types.Variable v : beanKey.getVariables()) {
			sw.write(String.valueOf(String.format("    public %1$s: %2$s; %3$s", v.getName(), TypeName.GetName(v.getVariableType()), v.getComment())) + System.lineSeparator());
		}
		sw.write("" + System.lineSeparator());

		{
		// params construct with init
			sw.write("     public constructor(" + GetParamListWithDefault(beanKey.getVariables()) + ") {" + System.lineSeparator());
			for (Types.Variable v : beanKey.getVariables()) {
				sw.write("        this." + v.getName() + " = " + v.getNamePrivate() + "_;" + System.lineSeparator());
			}
			sw.write("    }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
		}
		sw.write("    public static readonly TYPEID: bigint = " + beanKey.getTypeId() + "n;" + System.lineSeparator());
		sw.write("    public TypeId(): bigint { return " + beanKey.getSpace().Path("_", beanKey.getName()) + ".TYPEID; }" + System.lineSeparator());
		sw.WriteLine();
		sw.write("" + System.lineSeparator());
		Encode.Make(beanKey, sw, "    ");
		Decode.Make(beanKey, sw, "    ");
		sw.write("}" + System.lineSeparator());
	}
}