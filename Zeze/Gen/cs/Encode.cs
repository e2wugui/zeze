using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Encode : Types.Visitor
    {
        private string varname;
        private int id;
        private string bufname;
        private System.IO.StreamWriter sw;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void Encode(ByteBuffer _os_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + bean.Variables.Count + "); // Variables.Count");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Encode(v.NameUpper1, v.Id, "_os_", sw, prefix + "    "));
            }

            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Encode(ByteBuffer _os_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + bean.Variables.Count + "); // Variables.Count");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Encode(v.NamePrivate, v.Id, "_os_", sw, prefix + "    "));
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
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT);");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);");
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
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT);");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);");
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
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.BYTE | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteByte(" + varname + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.DOUBLE | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteDouble(" + varname + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.INT | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.LONG | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteLong(" + varname + ");");
        }

        public void Visit(TypeBool type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.BOOL | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBool(" + varname + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.BYTES | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBinary(" + varname + ");");
        }

        public void Visit(TypeString type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.STRING | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ");");
        }

        private void EncodeCollection(TypeCollection type)
        {
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.BeginWriteSegment(out var _state_);");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + varname + ".Count);");
            sw.WriteLine(prefix + "    foreach (var _v_ in " + varname + ")");
            sw.WriteLine(prefix + "    {");
            vt.Accept(new Encode("_v_", -1, "_os_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.EndWriteSegment(_state_); ");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeList type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.LIST | " + id + " << ByteBuffer.TAG_SHIFT);");
            EncodeCollection(type);
        }

        public void Visit(TypeSet type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.SET | " + id + " << ByteBuffer.TAG_SHIFT);");
            EncodeCollection(type);
        }

        public void Visit(TypeMap type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");

            Types.Type keytype = type.KeyType;
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + bufname + ".WriteInt(ByteBuffer.MAP | " + id + " << ByteBuffer.TAG_SHIFT);");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.BeginWriteSegment(out var _state_);");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(keytype) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + TypeTagName.GetName(valuetype) + ");");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + varname + ".Count);");
            sw.WriteLine(prefix + "    foreach (var _e_ in " + varname + ")");
            sw.WriteLine(prefix + "    {");
            keytype.Accept(new Encode("_e_.Key", -1, "_os_", sw, prefix + "        "));
            valuetype.Accept(new Encode("_e_.Value", -1, "_os_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.EndWriteSegment(_state_); ");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeFloat type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(ByteBuffer.FLOAT | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteFloat(" + varname + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(ByteBuffer.SHORT | " + id + " << ByteBuffer.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteShort(" + varname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id >= 0)
            {
                sw.WriteLine($"{prefix}{bufname}.WriteInt(ByteBuffer.DYNAMIC | {id} << ByteBuffer.TAG_SHIFT);");
                sw.WriteLine($"{prefix}{varname}.Encode({bufname});");
                /*
                sw.WriteLine($"{prefix}{bufname}.WriteLong8({varname}.TypeId);");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    " + bufname + ".BeginWriteSegment(out var _state_);");
                sw.WriteLine(prefix + "    " + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "    " + bufname + ".EndWriteSegment(_state_);");
                sw.WriteLine(prefix + "}");
                */
            }
            else
            {
                throw new Exception("invalie Variable.Id");
            }

        }
    }
}
