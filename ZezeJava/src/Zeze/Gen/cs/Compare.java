package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Compare implements Visitor {
	public static void Make(BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public int CompareTo(object _o1_)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    if (_o1_ == this) return 0;" + System.lineSeparator());
		sw.write(prefix + "    if (_o1_ is " + bean.getName() + " _o_)" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        int _c_" + (!bean.getVariables().isEmpty() ? ";" : " = 0;") + System.lineSeparator());
		for (Variable var : bean.getVariables()) {
			Compare e = new Compare(var, "_o_");
			var.getVariableType().Accept(e);
			sw.write(prefix + "        _c_ = " + e.text + ";" + System.lineSeparator());
			sw.write(prefix + "        if (0 != _c_) return _c_;" + System.lineSeparator());
		}
		sw.write(prefix + "        return _c_;" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    throw new System.Exception(\"CompareTo: another object is not " + bean.getFullName() + "\");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	private Variable variable;
	private String another;
	private String text;

	public Compare(Variable var, String another) {
		this.variable = var;
		this.another = another;
	}

	public final void Visit(Bean type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(BeanKey type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeByte type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeDouble type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeInt type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeLong type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeBool type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeBinary type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeString type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
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
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeShort type) {
		text = variable.getNamePrivate() + ".CompareTo(" + another + "." + variable.getNamePrivate() + ")";
	}

	public final void Visit(TypeDynamic type) {
		throw new UnsupportedOperationException();
	}
}