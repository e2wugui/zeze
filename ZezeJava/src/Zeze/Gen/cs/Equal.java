package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Equal implements Types.Visitor {
	private Types.Variable var;
	private String another;
	private boolean isEquals;
	private String text;

	/** 
	 实际上 BeanKey 很多类型都不支持，下面先尽量实现，以后可能用来实现 Bean 的 Equals.
	 
	 @param bean
	 @param sw
	 @param prefix
	*/
	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override bool Equals(object _obj1_)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    if (_obj1_ == this) return true;" + System.lineSeparator());
		sw.write(prefix + "    if (_obj1_ is " + bean.getName() + " _obj_)" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		for (Types.Variable var : bean.getVariables()) {
			var v = new Equal(var, "_obj_", false);
			var.getVariableType().Accept(v);
			sw.write(prefix + "        if (" + v.text + ") return false;" + System.lineSeparator());
		}
		sw.write(prefix + "        return true;" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    return false;" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Equal(Variable var, String another, boolean isEquals) {
		this.var = var;
		this.another = another;
		this.isEquals = isEquals;
	}

	public final void Visit(Bean type) {
		text = (isEquals ? "" : "!") + var.getNamePrivate() + ".Equals(" + another + "." + var.getNamePrivate() + ")";
	}

	public final void Visit(BeanKey type) {
		text = (isEquals ? "" : "!") + var.getNamePrivate() + ".Equals(" + another + "." + var.getNamePrivate() + ")";
	}

	public final void Visit(TypeByte type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeDouble type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeInt type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeLong type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeBool type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeBinary type) {
		throw new UnsupportedOperationException();
	}

	public final void Visit(TypeString type) {
		text = (isEquals ? "" : "!") + var.getNamePrivate() + ".Equals(" + another + "." + var.getNamePrivate() + ")";
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
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeShort type) {
		text = var.getNamePrivate() + (isEquals ? " == " : " != ") + another + "." + var.getNamePrivate();
	}

	public final void Visit(TypeDynamic type) {
		throw new UnsupportedOperationException();
	}
}