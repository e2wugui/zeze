package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Construct implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Variable variable;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public " + bean.getName() + "() : this(0)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
		sw.write(prefix + "public " + bean.getName() + "(int _varId_) : base(_varId_)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
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
			String varname = variable.getNamePrivate();
			sw.write(prefix + varname + " = " + value + ";" + System.lineSeparator());
		}
	}

	public final void Visit(Bean type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + variable.getNamePrivate() + " = new " + typeName + "(" + variable.getId() + ");" + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + variable.getNamePrivate() + " = new " + typeName + "();" + System.lineSeparator());
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
		Initial();
	}

	public final void Visit(TypeBool type) {
		Initial();
	}

	public final void Visit(TypeBinary type) {
		sw.write(prefix + variable.getNamePrivate() + " = Zeze.Net.Binary.Empty;" + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		String value = variable.getInitial();
		String varname = variable.getNamePrivate();
		sw.write(prefix + varname + " = \"" + value + "\";" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + variable.getNamePrivate() + " = new " + typeName + "(ObjectId + " + variable.getId() + ", _v => new Log_" + variable.getNamePrivate() + "(this, _v));" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + variable.getNamePrivate() + " = new " + typeName + "(ObjectId + " + variable.getId() + ", _v => new Log_" + variable.getNamePrivate() + "(this, _v));" + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + variable.getNamePrivate() + " = new " + typeName + "(ObjectId + " + variable.getId() + ", _v => new Log_" + variable.getNamePrivate() + "(this, _v));" + System.lineSeparator());
		var key = TypeName.GetName(type.getKeyType());
		var value = type.getValueType().isNormalBean() ? TypeName.GetName(type.getValueType()) + "ReadOnly" : TypeName.GetName(type.getValueType());
		var readonlyTypeName = String.format("Zeze.Transaction.Collections.PMapReadOnly<%1$s,%2$s,%3$s>", key, value, TypeName.GetName(type.getValueType()));
		sw.write(String.format("%1$s%2$sReadOnly = new %3$s(%4$s);", prefix, variable.getNamePrivate(), readonlyTypeName, variable.getNamePrivate()) + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		Initial();
	}

	public final void Visit(TypeShort type) {
		Initial();
	}

	public final void Visit(TypeDynamic type) {
		sw.write(prefix + variable.getNamePrivate() + " = new Zeze.Transaction.DynamicBean" + String.format("(%1$s, GetSpecialTypeIdFromBean_%2$s, CreateBeanFromSpecialTypeId_%3$s);", variable.getId(), variable.getNameUpper1(), variable.getNameUpper1()) + System.lineSeparator());
	}
}