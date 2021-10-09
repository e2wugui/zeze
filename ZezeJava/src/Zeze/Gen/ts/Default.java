package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;

public class Default implements Types.Visitor {
	private Types.Variable variable;
	private String Value;
	public final String getValue() {
		return Value;
	}
	private void setValue(String value) {
		Value = value;
	}

	public static String GetDefault(Types.Variable var) {
		Default def = new Default(var);
		var.getVariableType().Accept(def);
		return def.getValue();
	}

	private Default(Types.Variable var) {
		this.variable = var;
	}

	private void SetDefaultValue(String def) {
		setValue((variable.getInitial().length() > 0) ? variable.getInitial() : def);
	}

	public final void Visit(Bean type) {
		setValue("null");
	}

	public final void Visit(BeanKey type) {
		setValue("null");
	}

	public final void Visit(TypeByte type) {
		SetDefaultValue("0");
	}

	public final void Visit(TypeShort type) {
		SetDefaultValue("0");
	}

	public final void Visit(TypeInt type) {
		setValue("null");
	}

	public final void Visit(TypeLong type) {
		SetDefaultValue("0n");
	}

	public final void Visit(TypeBool type) {
		SetDefaultValue("false");
	}

	public final void Visit(TypeBinary type) {
		setValue("null");
	}

	public final void Visit(TypeString type) {
		SetDefaultValue("\"\"");
	}

	public final void Visit(TypeFloat type) {
		SetDefaultValue("0");
	}

	public final void Visit(TypeDouble type) {
		SetDefaultValue("0");
	}

	public final void Visit(TypeList type) {
		setValue("null");
	}

	public final void Visit(TypeSet type) {
		setValue("null");
	}

	public final void Visit(TypeMap type) {
		setValue("null");
	}

	public final void Visit(TypeDynamic type) {
		setValue("null");
	}
}