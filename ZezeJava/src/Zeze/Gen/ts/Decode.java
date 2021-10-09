package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Decode implements Types.Visitor {
	private String varname;
	private int id;
	private String bufname;
	private OutputStreamWriter sw;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public Decode(_os_: Zeze.ByteBuffer): void {" + System.lineSeparator());
		sw.write(prefix + "    for (var _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) // Variables.Count" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        var _tagid_ = _os_.ReadInt();" + System.lineSeparator());
		sw.write(prefix + "        switch (_tagid_)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Decode("this." + v.getName(), v.getId(), "_os_", sw, prefix + "            "));
		}

		sw.write(prefix + "            default:" + System.lineSeparator());
		sw.write(prefix + "                Zeze.ByteBuffer.SkipUnknownField(_tagid_, _os_);" + System.lineSeparator());
		sw.write(prefix + "                break;" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public Decode(_os_: Zeze.ByteBuffer): void {" + System.lineSeparator());
		sw.write(prefix + "    for (var _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) // Variables.Count" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        var _tagid_ = _os_.ReadInt();" + System.lineSeparator());
		sw.write(prefix + "        switch (_tagid_)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Decode("this." + v.getName(), v.getId(), "_os_", sw, prefix + "            "));
		}

		sw.write(prefix + "            default:" + System.lineSeparator());
		sw.write(prefix + "                Zeze.ByteBuffer.SkipUnknownField(_tagid_, _os_);" + System.lineSeparator());
		sw.write(prefix + "                break;" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Decode(String varname, int id, String bufname, OutputStreamWriter sw, String prefix) {
		this.varname = varname;
		this.id = id;
		this.bufname = bufname;
		this.sw = sw;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    {" + System.lineSeparator());
			sw.write(prefix + "        var _state_ = " + bufname + ".BeginReadSegment();" + System.lineSeparator());
			sw.write(prefix + "        " + varname + ".Decode(" + bufname + ");" + System.lineSeparator());
			sw.write(prefix + "        " + bufname + ".EndReadSegment(_state_);" + System.lineSeparator());
			sw.write(prefix + "    }" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + ".Decode(" + bufname + ");" + System.lineSeparator());
		}
	}

	public final void Visit(BeanKey type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    {" + System.lineSeparator());
			sw.write(prefix + "        var _state_ = " + bufname + ".BeginReadSegment();" + System.lineSeparator());
			sw.write(prefix + "        " + varname + ".Decode(" + bufname + ");" + System.lineSeparator());
			sw.write(prefix + "        " + bufname + ".EndReadSegment(_state_);" + System.lineSeparator());
			sw.write(prefix + "    }" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + ".Decode(" + bufname + ");" + System.lineSeparator());
		}
	}

	public final void Visit(TypeByte type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.BYTE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadByte();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadByte();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeDouble type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.DOUBLE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadDouble();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadDouble();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeInt type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.INT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadInt();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadInt();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeLong type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.LONG | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadLong();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadLong();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeBool type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.BOOL | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadBool();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadBool();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeBinary type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.BYTES | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadBytes();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadBytes();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeString type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.STRING | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadString();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadString();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeList type) {
		if (id < 0) {
			throw new RuntimeException("invalid variable.id");
		}

		sw.write(prefix + "case (Zeze.ByteBuffer.LIST | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):" + System.lineSeparator());
		Types.Type valuetype = type.getValueType();

		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        var _state_ = _os_.BeginReadSegment();" + System.lineSeparator());
		sw.write(prefix + "        _os_.ReadInt(); // skip collection.value typetag" + System.lineSeparator());
		sw.write(prefix + "        " + varname + " = new " + TypeName.GetName(type) + "();" + System.lineSeparator());
		sw.write(prefix + "        for (var _size_ = _os_.ReadInt(); _size_ > 0; --_size_)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		String vartmpname = Program.GenUniqVarName();
		valuetype.Accept(new Define(vartmpname, sw, prefix + "            "));
		valuetype.Accept(new Decode(vartmpname, -1, "_os_", sw, prefix + "            "));
		sw.write(prefix + "            " + varname + ".push(" + vartmpname + ");" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        _os_.EndReadSegment(_state_);" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    break;" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		if (id < 0) {
			throw new RuntimeException("invalid variable.id");
		}

		sw.write(prefix + "case (Zeze.ByteBuffer.SET | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):" + System.lineSeparator());
		Types.Type valuetype = type.getValueType();

		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        var _state_ = _os_.BeginReadSegment();" + System.lineSeparator());
		sw.write(prefix + "        _os_.ReadInt(); // skip collection.value typetag" + System.lineSeparator());
		sw.write(prefix + "        " + varname + ".clear();" + System.lineSeparator());
		sw.write(prefix + "        for (var _size_ = _os_.ReadInt(); _size_ > 0; --_size_)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		String vartmpname = Program.GenUniqVarName();
		valuetype.Accept(new Define(vartmpname, sw, prefix + "            "));
		valuetype.Accept(new Decode(vartmpname, -1, "_os_", sw, prefix + "            "));
		sw.write(prefix + "            " + varname + ".add(" + vartmpname + ");" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        _os_.EndReadSegment(_state_);" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    break;" + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		if (id < 0) {
			throw new RuntimeException("invalid variable.id");
		}

		Types.Type keytype = type.getKeyType();
		Types.Type valuetype = type.getValueType();

		sw.write(prefix + "case (Zeze.ByteBuffer.MAP | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        var _state_ = _os_.BeginReadSegment();" + System.lineSeparator());
		sw.write(prefix + "        _os_.ReadInt(); // skip key typetag" + System.lineSeparator());
		sw.write(prefix + "        _os_.ReadInt(); // skip value typetag" + System.lineSeparator());
		sw.write(prefix + "        " + varname + ".clear();" + System.lineSeparator());
		sw.write(prefix + "        for (var size = _os_.ReadInt(); size > 0; --size)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		String vartmpkey = Program.GenUniqVarName();
		String vartmpvalue = Program.GenUniqVarName();
		keytype.Accept(new Define(vartmpkey, sw, prefix + "            "));
		keytype.Accept(new Decode(vartmpkey, -1, "_os_", sw, prefix + "            "));
		valuetype.Accept(new Define(vartmpvalue, sw, prefix + "            "));
		valuetype.Accept(new Decode(vartmpvalue, -1, "_os_", sw, prefix + "            "));
		sw.write(prefix + "            " + varname + ".set(" + vartmpkey + ", " + vartmpvalue + ");" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        _os_.EndReadSegment(_state_);" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    break;" + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.FLOAT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadFloat();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadFloat();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeShort type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.SHORT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + "    " + varname + " = " + bufname + ".ReadShort();" + System.lineSeparator());
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + " = " + bufname + ".ReadShort();" + System.lineSeparator());
		}
	}

	public final void Visit(TypeDynamic type) {
		if (id >= 0) {
			sw.write(prefix + "case (Zeze.ByteBuffer.DYNAMIC | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): " + System.lineSeparator());
			sw.write(prefix + String.format("    %1$s.Decode(%2$s);", varname, bufname) + System.lineSeparator());
			/*
			sw.WriteLine(prefix + "    switch (" + bufname + ".ReadLong8())");
			sw.WriteLine(prefix + "    {");
			foreach (Bean real in type.RealBeans)
			{
			    string realName = TypeName.GetName(real);
			    sw.WriteLine(prefix + "        case " + realName + ".TYPEID:");
			    sw.WriteLine(prefix + "            {");
			    sw.WriteLine(prefix + "                var _state_ = " + bufname + ".BeginReadSegment();");
			    sw.WriteLine(prefix + "                " + varname + " = new " + realName + "();");
			    sw.WriteLine(prefix + "                " + varname + ".Decode(" + bufname + ");");
			    sw.WriteLine(prefix + "                " + bufname + ".EndReadSegment(_state_);");
			    sw.WriteLine(prefix + "            }");
			    sw.WriteLine(prefix + "            break;");
			}
			sw.WriteLine(prefix + "        case Zeze.EmptyBean.TYPEID:");
			sw.WriteLine(prefix + "            " + varname + " = new Zeze.EmptyBean();");
			sw.WriteLine(prefix + "            " + bufname + ".SkipBytes();");
			sw.WriteLine(prefix + "            break;");
			sw.WriteLine(prefix + "        default:");
			sw.WriteLine(prefix + "            " + bufname + ".SkipBytes();");
			sw.WriteLine(prefix + "            break;");
			sw.WriteLine(prefix + "    }");
			*/
			sw.write(prefix + "    break;" + System.lineSeparator());
		}
		else {
			throw new RuntimeException("invalie Variable.Id");
		}
	}
}