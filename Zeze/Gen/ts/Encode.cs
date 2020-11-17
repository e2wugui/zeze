using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Encode : Types.Visitor
    {
        private string varname;
        private string bufname;
        private int id;
        private System.IO.StreamWriter sw;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Encode(_os_: Zeze.ByteBuffer): void {");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + bean.Variables.Count + "); // Variables.Count");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Encode("this." + v.Name, v.Id, "_os_", sw, prefix + "    "));
            }

            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Encode(_os_: Zeze.ByteBuffer): void");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + bean.Variables.Count + "); // Variables.Count");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Encode("this." + v.Name, v.Id, "_os_", sw, prefix + "    "));
            }

            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Encode(string varname, int id, string bufname, System.IO.StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();");
                sw.WriteLine(prefix + "    " + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "    " + bufname + ".EndWriteSegment(_state_);");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
            }
        }

        public void Visit(BeanKey type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();");
                sw.WriteLine(prefix + "    " + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "    " + bufname + ".EndWriteSegment(_state_);");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
            }
        }

        public void Visit(TypeByte type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BYTE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteByte(" + varname + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.DOUBLE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteDouble(" + varname + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.INT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.LONG | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteLong(" + varname + ");");
        }

        public void Visit(TypeBool type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BOOL | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBool(" + varname + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.BYTES | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBytes(" + varname + ");");
        }

        public void Visit(TypeString type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.STRING | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ");");
        }

        public void Visit(TypeList type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.LIST | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    var _state_ = _os_.BeginWriteSegment();");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + varname + ".length);");
            string vartmpname = Program.GenUniqVarName();
            sw.WriteLine(prefix + "    for (var " + vartmpname + " in " + varname + ")");
            sw.WriteLine(prefix + "    {");
            vt.Accept(new Encode(varname + "[" + vartmpname + "]", -1, "_os_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.EndWriteSegment(_state_); ");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeSet type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.SET | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    var _state_ = _os_.BeginWriteSegment();");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + varname + ".size);");
            string tmpvarname = Program.GenUniqVarName();
            sw.WriteLine(prefix + "    for (let " + tmpvarname + " of " + varname + ")");
            sw.WriteLine(prefix + "    {");
            vt.Accept(new Encode(tmpvarname, -1, "_os_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.EndWriteSegment(_state_); ");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeMap type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");

            Types.Type keytype = type.KeyType;
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + bufname + ".WriteInt(Zeze.ByteBuffer.MAP | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    var _state_ = _os_.BeginWriteSegment();");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(keytype) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(valuetype) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + varname + ".size);");
            string tmpvarname = Program.GenUniqVarName();
            sw.WriteLine(prefix + "    for (let " + tmpvarname + " of " + varname + ".entries())");
            sw.WriteLine(prefix + "    {");
            keytype.Accept(new Encode("" + tmpvarname + "[0]", -1, "_os_", sw, prefix + "        "));
            valuetype.Accept(new Encode("" + tmpvarname + "[1]", -1, "_os_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.EndWriteSegment(_state_); ");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeFloat type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(Zeze.ByteBuffer.FLOAT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteFloat(" + varname + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(Zeze.ByteBuffer.SHORT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteShort(" + varname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id >= 0)
            {
                sw.WriteLine($"{prefix}{bufname}.WriteInt(Zeze.ByteBuffer.DYNAMIC | {id} << Zeze.ByteBuffer.TAG_SHIFT);");
                sw.WriteLine($"{prefix}{bufname}.WriteLong8({varname}.TypeId());");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    var _state_ = " + bufname + ".BeginWriteSegment();");
                sw.WriteLine(prefix + "    " + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "    " + bufname + ".EndWriteSegment(_state_);");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                throw new Exception("invalie Variable.Id");
            }

        }
    }
}
