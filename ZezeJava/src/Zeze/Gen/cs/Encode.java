package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Encode implements Types.Visitor {
	private String varname;
	private int id;
	private String bufname;
	private OutputStreamWriter sw;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override void Encode(ByteBuffer _os_)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + bean.getVariables().size() + "); // Variables.Count" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Encode(v.getNameUpper1(), v.getId(), "_os_", sw, prefix + "    "));
		}

		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public void Encode(ByteBuffer _os_)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + bean.getVariables().size() + "); // Variables.Count" + System.lineSeparator());

		for (Types.Variable v : bean.getVariables()) {
			v.getVariableType().Accept(new Encode(v.getNamePrivate(), v.getId(), "_os_", sw, prefix + "    "));
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
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			sw.write(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);" + System.lineSeparator());
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
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			sw.write(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);" + System.lineSeparator());
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
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.BYTE | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteByte(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeDouble type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.DOUBLE | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteDouble(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeInt type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.INT | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteInt(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeLong type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.LONG | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteLong(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.BOOL | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteBool(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeBinary type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.BYTES | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteBinary(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		if (id >= 0) {
			sw.write(prefix + bufname + ".WriteInt(ByteBuffer.STRING | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + bufname + ".WriteString(" + varname + ");" + System.lineSeparator());
	}

	private void EncodeCollection(TypeCollection type) {
		Types.Type vt = type.getValueType();
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    _os_.BeginWriteSegment(out var _state_);" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + varname + ".Count);" + System.lineSeparator());
		sw.write(prefix + "    foreach (var _v_ in " + varname + ")" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		vt.Accept(new Encode("_v_", -1, "_os_", sw, prefix + "        "));
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    _os_.EndWriteSegment(_state_); " + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}
		sw.write(prefix + bufname + ".WriteInt(ByteBuffer.LIST | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		EncodeCollection(type);
	}

	public final void Visit(TypeSet type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}
		sw.write(prefix + bufname + ".WriteInt(ByteBuffer.SET | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		EncodeCollection(type);
	}

	public final void Visit(TypeMap type) {
		if (id < 0) {
			throw new RuntimeException("invalie Variable.Id");
		}

		Types.Type keytype = type.getKeyType();
		Types.Type valuetype = type.getValueType();

		sw.write(prefix + bufname + ".WriteInt(ByteBuffer.MAP | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    _os_.BeginWriteSegment(out var _state_);" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(keytype) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(valuetype) + ");" + System.lineSeparator());
		sw.write(prefix + "    _os_.WriteInt(" + varname + ".Count);" + System.lineSeparator());
		sw.write(prefix + "    foreach (var _e_ in " + varname + ")" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		keytype.Accept(new Encode("_e_.Key", -1, "_os_", sw, prefix + "        "));
		valuetype.Accept(new Encode("_e_.Value", -1, "_os_", sw, prefix + "        "));
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    _os_.EndWriteSegment(_state_); " + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		if (id >= 0) {
			sw.write(prefix + "_os_.WriteInt(ByteBuffer.FLOAT | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + "_os_.WriteFloat(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeShort type) {
		if (id >= 0) {
			sw.write(prefix + "_os_.WriteInt(ByteBuffer.SHORT | " + id + " << ByteBuffer.TAG_SHIFT);" + System.lineSeparator());
		}
		sw.write(prefix + "_os_.WriteShort(" + varname + ");" + System.lineSeparator());
	}

	public final void Visit(TypeDynamic type) {
		if (id >= 0) {
			sw.write(String.valueOf(String.format("%1$s%2$s.WriteInt(ByteBuffer.DYNAMIC | %3$s << ByteBuffer.TAG_SHIFT);", prefix, bufname, id)) + System.lineSeparator());
			sw.write(String.valueOf(String.format("%1$s%2$s.Encode(%3$s);", prefix, varname, bufname)) + System.lineSeparator());
			/*
			sw.WriteLine($"{prefix}{bufname}.WriteLong8({varname}.TypeId);");
			sw.WriteLine(prefix + "{");
			sw.WriteLine(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);");
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