package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Define implements Types.Visitor {
	private String varname;
	private OutputStreamWriter sw;
	private String prefix;

	public Define(String varname, OutputStreamWriter sw, String prefix) {
		this.varname = varname;
		this.sw = sw;
		this.prefix = prefix;
	}

	private void DefineNew(Types.Type type) {
		String tName = TypeName.GetName(type);
		sw.write(prefix + "var " + varname + ": " + tName + " = new " + tName + "();" + System.lineSeparator());
	}

	private void DefineStack(Types.Type type) {
		String typeName = TypeName.GetName(type);
		sw.write(prefix + "var " + varname + ": " + typeName + ";" + System.lineSeparator());
	}

	public final void Visit(Bean type) {
		DefineNew(type);
	}

	public final void Visit(BeanKey type) {
		DefineNew(type);
	}

	public final void Visit(TypeByte type) {
		DefineStack(type);
	}

	public final void Visit(TypeDouble type) {
		DefineStack(type);
	}

	public final void Visit(TypeInt type) {
		DefineStack(type);
	}

	public final void Visit(TypeLong type) {
		DefineStack(type);
	}

	public final void Visit(TypeBool type) {
		DefineStack(type);
	}

	public final void Visit(TypeBinary type) {
		DefineStack(type);
	}

	public final void Visit(TypeString type) {
		DefineStack(type);
	}

	public final void Visit(TypeList type) {
		DefineNew(type);
	}

	public final void Visit(TypeSet type) {
		DefineNew(type);
	}

	public final void Visit(TypeMap type) {
		DefineNew(type);
	}

	public final void Visit(TypeFloat type) {
		DefineStack(type);
	}

	public final void Visit(TypeShort type) {
		DefineStack(type);
	}

	public final void Visit(TypeDynamic type) {
		DefineStack(type);
	}
}