package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Construct implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Variable variable;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public constructor() {" + System.lineSeparator());
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new Construct(sw, var, prefix + "    "));
		}
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Construct(OutputStreamWriter sw, Types.Variable variable, String prefix) {
		this.sw = sw;
		this.variable = variable;
		this.prefix = prefix;
	}

	private void Initial() {
		String value = variable.getInitial();
		if (value.length() > 0) {
			String varname = variable.getName();
			sw.write(prefix + "this." + varname + " = " + value + ";" + System.lineSeparator());
		}
	}

	public final void Visit(Bean type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "this." + variable.getName() + " = new " + typeName + "();" + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "this." + variable.getName() + " = new " + typeName + "();" + System.lineSeparator());
	}

	public final void Visit(TypeByte type) {
		Initial();
	}

	public final void Visit(TypeDouble type) {
		Initial();
	}

	public final void Visit(TypeInt type) {
		Initial();
	}

	public final void Visit(TypeLong type) {
		long init = variable.getInitial().length() > 0 ? Long.parseLong(variable.getInitial()) : 0;
		sw.write(prefix + "this." + variable.getName() + " = " + init + "n;" + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
		Initial();
	}

	public final void Visit(TypeBinary type) {
		sw.write(prefix + "this." + variable.getName() + " = new Uint8Array(0);" + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		String value = variable.getInitial();
		String varname = variable.getName();
		sw.write(prefix + "this." + varname + " = \"" + value + "\";" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "this." + variable.getName() + " = new " + typeName + "();" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "this." + variable.getName() + " = new " + typeName + "();" + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "this." + variable.getName() + " = new " + typeName + "();" + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		Initial();
	}

	public final void Visit(TypeShort type) {
		Initial();
	}

	public final void Visit(TypeDynamic type) {
		Zeze.Gen.Types.Type tempVar = variable.getBean();
		var bean = tempVar instanceof Bean ? (Bean)tempVar : null;
		sw.write(prefix + "this." + variable.getName() + " = new Zeze.DynamicBean(" + String.format("%1$s.GetSpecialTypeIdFromBean_%2$s, ", bean.getSpace().Path("_", bean.getName()), variable.getNameUpper1()) + String.format("%1$s.CreateBeanFromSpecialTypeId_%2$s", bean.getSpace().Path("_", bean.getName()), variable.getNameUpper1()) + ");" + System.lineSeparator());
	}
}