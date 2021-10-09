package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Assign implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Variable var;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public void Assign(" + bean.getName() + " other)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new Assign(var, sw, prefix + "    "));
		}
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Assign(Types.Variable var, OutputStreamWriter sw, String prefix) {
		this.var = var;
		this.sw = sw;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		sw.write(prefix + var.getNameUpper1() + ".Assign(other." + var.getNameUpper1() + ");" + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeByte type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeDouble type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeInt type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeLong type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeBinary type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		sw.write(prefix + var.getNameUpper1() + ".Clear();" + System.lineSeparator());
		String copyif = type.getValueType().isNormalBean() ? "e.Copy()" : "e";

		sw.write(prefix + "foreach (var e in other." + var.getNameUpper1() + ")" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    " + var.getNameUpper1() + ".Add(" + copyif + ");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		sw.write(prefix + var.getNameUpper1() + ".Clear();" + System.lineSeparator());
		String copyif = type.getValueType().isNormalBean() ? "e.Copy()" : "e"; // set 里面现在不让放 bean，先这样写吧。

		sw.write(prefix + "foreach (var e in other." + var.getNameUpper1() + ")" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    " + var.getNameUpper1() + ".Add(" + copyif + ");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		sw.write(prefix + var.getNameUpper1() + ".Clear();" + System.lineSeparator());
		String copyif = type.getValueType().isNormalBean() ? "e.Value.Copy()" : "e.Value";

		sw.write(prefix + "foreach (var e in other." + var.getNameUpper1() + ")" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    " + var.getNameUpper1() + ".Add(e.Key, " + copyif + ");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeShort type) {
		sw.write(prefix + var.getNameUpper1() + " = other." + var.getNameUpper1() + ";" + System.lineSeparator());
	}

	public final void Visit(TypeDynamic type) {
		sw.write(prefix + var.getNameUpper1() + ".Assign(other." + var.getNameUpper1() + ");" + System.lineSeparator());
	}
}