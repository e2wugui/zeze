package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Encode implements Types.Visitor {
	private String varname;
	private String bufname;
	private int id;
	private OutputStreamWriter sw;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public Encode(_os_: Zeze.ByteBuffer): void {" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + bean.getVariables().size() + "); // Variables.Count" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Encode("this." + v.getName(), v.getId(), "_os_", sw, prefix + "    "));
		}

		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public Encode(_os_: Zeze.ByteBuffer): void" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + bean.getVariables().size() + "); // Variables.Count" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Encode("this." + v.getName(), v.getId(), "_os_", sw, prefix + "    "));
		}

		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Encode(String varname, int id, String bufname, OutputStreamWriter sw, String prefix) {
		this.varname = varname;
		this.id = id;
		this.bufname = bufname;
		this.sw = sw;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			sw.write(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();" + System.lineSeparator());
			sw.write(prefix + "    " + varname + ".Encode(" + bufname + ");" + System.lineSeparator());
			sw.write(prefix + "    " + bufname + ".EndWriteSegment(_state_);" + System.lineSeparator());
			sw.write(prefix + "}" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + ".Encode(" + bufname + ");" + System.lineSeparator());
		}
	}

	public final void Visit(BeanKey type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			sw.write(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();" + System.lineSeparator());
			sw.write(prefix + "    " + varname + ".Encode(" + bufname + ");" + System.lineSeparator());
			sw.write(prefix + "    " + bufname + ".EndWriteSegment(_state_);" + System.lineSeparator());
			sw.write(prefix + "}" + System.lineSeparator());
		}
		else {
			sw.write(prefix + varname + ".Encode(" + bufname + ");" + System.lineSeparator());
		}
	}

	public final void Visit(TypeByte type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BYTE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteByte(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeDouble type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.DOUBLE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteDouble(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeInt type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.INT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteInt(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeLong type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.LONG | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteLong(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BOOL | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteBool(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeBinary type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BYTES | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteBytes(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.STRING | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteString(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}
		sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.LIST | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		Types.Type vt = type.getValueType();
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    var _state_ = _os_.BeginWriteSegment();" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + varname + ".length);" + System.lineSeparator());
		String vartmpname = Program.GenUniqVarName();
		sw.write(prefix + "    for (var " + vartmpname + " in " + varname + ")" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		vt.Accept(new Encode(varname + "[" + vartmpname + "]", -1, "_os_", sw, prefix + "        "));
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    _os_.EndWriteSegment(_state_); " + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}
		sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.SET | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		Types.Type vt = type.getValueType();
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    var _state_ = _os_.BeginWriteSegment();" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + varname + ".size);" + System.lineSeparator());
		String tmpvarname = Program.GenUniqVarName();
		sw.write(prefix + "    for (let " + tmpvarname + " of " + varname + ")" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		vt.Accept(new Encode(tmpvarname, -1, "_os_", sw, prefix + "        "));
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    _os_.EndWriteSegment(_state_); " + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}

		Types.Type keytype = type.getKeyType();
		Types.Type valuetype = type.getValueType();

		sw.write(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.MAP | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    var _state_ = _os_.BeginWriteSegment();" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(keytype) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(valuetype) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + varname + ".size);" + System.lineSeparator());
		String tmpvarname = Program.GenUniqVarName();
		sw.write(prefix + "    for (let " + tmpvarname + " of " + varname + ".entries())" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		keytype.Accept(new Encode("" + tmpvarname + "[0]", -1, "_os_", sw, prefix + "        "));
		valuetype.Accept(new Encode("" + tmpvarname + "[1]", -1, "_os_", sw, prefix + "        "));
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    _os_.EndWriteSegment(_state_); " + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		if (id >= 0) {
			sw.write(prefix + "_os_.WriteInt(Zeze.ByteBuffer.FLOAT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + "_os_.WriteFloat(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeShort type) {
		if (id >= 0) {
			sw.write(prefix + "_os_.WriteInt(Zeze.ByteBuffer.SHORT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + "_os_.WriteShort(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeDynamic type) {
		if (id >= 0) {
			sw.write(String.valueOf(String.format("%1$s%2$s.WriteInt(Zeze.ByteBuffer.DYNAMIC | %3$s << Zeze.ByteBuffer.TAG_SHIFT);", prefix, bufname, id)) + System.lineSeparator());
			sw.write(String.valueOf(String.format("%1$s%2$s.Encode(%3$s);", prefix, varname, bufname)) + System.lineSeparator());
			/*
			sw.WriteLine($"{prefix}{bufname}.WriteLong8({varname}.TypeId());");
			sw.WriteLine(prefix + "{");
			sw.WriteLine(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();");
			sw.WriteLine(prefix + "    " + varname + ".Encode(" + bufname + ");");
			sw.WriteLine(prefix + "    " + bufname + ".EndWriteSegment(_state_);");
			sw.WriteLine(prefix + "}");
			*/
		}
		else {
			throw new RuntimeException("invalie Variable.Id");
		}

	}
}