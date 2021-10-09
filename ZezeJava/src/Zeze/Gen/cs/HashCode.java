package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class HashCode implements Visitor {
	public static void Make(BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override int GetHashCode()" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    const int _prime_ = 31;" + System.lineSeparator());
		sw.write(prefix + "    int _h_ = 0;" + System.lineSeparator());
		for (Variable var : bean.getVariables()) {
			HashCode e = new HashCode(var.getNamePrivate());
			var.getVariableType().Accept(e);
			sw.write(prefix + "    _h_ = _h_ * _prime_ + " + e.text + ";" + System.lineSeparator());
		}
		sw.write(prefix + "    return _h_;" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	private String varname;
	private String text;

	public HashCode(String varname) {
		this.varname = varname;
	}

	public final void Visit(Bean type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(BeanKey type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeByte type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeDouble type) {
		//text = "(int)System.BitConverter.DoubleToInt64Bits(" + varname + ")";
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeInt type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeLong type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeBool type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeBinary type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeString type) {
		text = varname + ".GetHashCode()";
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
		text = "System.BitConverter.SingleToInt32Bits(" + varname + ").GetHashCode()";
	}

	public final void Visit(TypeShort type) {
		text = varname + ".GetHashCode()";
	}

	public final void Visit(TypeDynamic type) {
		throw new UnsupportedOperationException();
	}
}