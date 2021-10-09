package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class PropertyReadOnly implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Variable var;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(String.valueOf(String.format("%1$spublic long TypeId { get; }", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$spublic void Encode(Zeze.Serialize.ByteBuffer _os_);", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$spublic bool NegativeCheck();", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$spublic Zeze.Transaction.Bean CopyBean();", prefix)) + System.lineSeparator());
		sw.WriteLine();
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new PropertyReadOnly(sw, var, prefix));
		}
	}

	public PropertyReadOnly(OutputStreamWriter sw, Types.Variable var, String prefix) {
		this.sw = sw;
		this.var = var;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + "ReadOnly " + var.getNameUpper1() + " { get; }" + System.lineSeparator());
	}

	private void WriteProperty(Types.Type type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + " { get; }" + System.lineSeparator());
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
		WriteProperty(type);
	}

	public final void Visit(TypeString type) {
		WriteProperty(type);
	}

	public final void Visit(TypeList type) {
		var valueName = type.getValueType().isNormalBean() ? TypeName.GetName(type.getValueType()) + "ReadOnly" : TypeName.GetName(type.getValueType());
		sw.write(prefix + "public System.Collections.Generic.IReadOnlyList<" + valueName + ">" + var.getNameUpper1() + " { get; }" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		var v = TypeName.GetName(type.getValueType());
		var t = String.format("System.Collections.Generic.IReadOnlySet<%1$s>", v);
		sw.write(String.valueOf(String.format("%1$spublic %2$s %3$s { get; }", prefix, t, var.getNameUpper1())) + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		var valueName = type.getValueType().isNormalBean() ? TypeName.GetName(type.getValueType()) + "ReadOnly" : TypeName.GetName(type.getValueType());
		var keyName = TypeName.GetName(type.getKeyType());
		sw.write(String.format("%1$s public System.Collections.Generic.IReadOnlyDictionary<%2$s,%3$s> %4$s { get; }", prefix, keyName, valueName, var.getNameUpper1()) + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		WriteProperty(type);
	}

	public final void Visit(TypeShort type) {
		WriteProperty(type);
	}

	public final void Visit(TypeDynamic type) {
		sw.write(String.valueOf(String.format("%1$spublic %2$sReadOnly %3$s { get; }", prefix, TypeName.GetName(type), var.getNameUpper1())) + System.lineSeparator());
		sw.WriteLine();
		for (Bean real : type.getRealBeans().values()) {
			String rname = TypeName.GetName(real);
			sw.write(prefix + "public " + rname + "ReadOnly " + var.getNameUpper1() + "_" + real.getSpace().Path("_", real.getName()) + " { get; }" + System.lineSeparator());
		}
	}
}