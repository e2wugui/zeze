package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class PropertyBeanKey implements Visitor {
	private OutputStreamWriter sw;
	private Types.Variable var;
	private String prefix;

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new PropertyBeanKey(sw, var, prefix));
		}
	}

	public PropertyBeanKey(OutputStreamWriter sw, Types.Variable var, String prefix) {
		this.sw = sw;
		this.var = var;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		throw new UnsupportedOperationException();
	}

	private void WriteProperty(Types.Type type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		WriteProperty(type);
	}

	public final void Visit(TypeByte type) {
		WriteProperty(type);
	}

	public final void Visit(TypeDouble type) {
		WriteProperty(type);
	}

	public final void Visit(TypeInt type) {
		WriteProperty(type);
	}

	public final void Visit(TypeLong type) {
		WriteProperty(type);
	}

	public final void Visit(TypeBool type) {
		WriteProperty(type);
	}

	public final void Visit(TypeBinary type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeString type) {
		WriteProperty(type);
	}

	public final void Visit(TypeList type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeSet type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeMap type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeFloat type) {
		WriteProperty(type);
	}

	public final void Visit(TypeShort type) {
		WriteProperty(type);
	}

	public final void Visit(TypeDynamic type) {
		throw new UnsupportedOperationException();
	}
}