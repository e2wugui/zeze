using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Decode : Types.Visitor
    {
        private string varname;
        private int id;
        private string bufname;
        private System.IO.StreamWriter sw;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Decode(_os_: Zeze.ByteBuffer): void {");
            sw.WriteLine(prefix + "    for (var _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) // Variables.Count");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _tagid_ = _os_.ReadInt();");
            sw.WriteLine(prefix + "        switch (_tagid_)");
            sw.WriteLine(prefix + "        {");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_os_", sw, prefix + "            "));
            }

            sw.WriteLine(prefix + "            default:");
            sw.WriteLine(prefix + "                Zeze.ByteBuffer.SkipUnknownField(_tagid_, _os_);");
            sw.WriteLine(prefix + "                break;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Decode(_os_: Zeze.ByteBuffer): void {");
            sw.WriteLine(prefix + "    for (var _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) // Variables.Count");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _tagid_ = _os_.ReadInt();");
            sw.WriteLine(prefix + "        switch (_tagid_)");
            sw.WriteLine(prefix + "        {");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_os_", sw, prefix + "            "));
            }

            sw.WriteLine(prefix + "            default:");
            sw.WriteLine(prefix + "                Zeze.ByteBuffer.SkipUnknownField(_tagid_, _os_);");
            sw.WriteLine(prefix + "                break;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Decode(string varname, int id, string bufname, System.IO.StreamWriter sw, string prefix)
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
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    {");
                sw.WriteLine(prefix + "        var _state_ = " + bufname + ".BeginReadSegment();");
                sw.WriteLine(prefix + "        " + varname + ".Decode(" + bufname + ");");
                sw.WriteLine(prefix + "        " + bufname + ".EndReadSegment(_state_);");
                sw.WriteLine(prefix + "    }");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
            }
        }

        public void Visit(BeanKey type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.BEAN | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    {");
                sw.WriteLine(prefix + "        var _state_ = " + bufname + ".BeginReadSegment();");
                sw.WriteLine(prefix + "        " + varname + ".Decode(" + bufname + ");");
                sw.WriteLine(prefix + "        " + bufname + ".EndReadSegment(_state_);");
                sw.WriteLine(prefix + "    }");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
            }
        }

        public void Visit(TypeByte type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.BYTE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadByte();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadByte();");
            }
        }

        public void Visit(TypeDouble type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.DOUBLE | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadDouble();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadDouble();");
            }
        }

        public void Visit(TypeInt type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.INT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadInt();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadInt();");
            }
        }

        public void Visit(TypeLong type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.LONG | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadLong();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadLong();");
            }
        }

        public void Visit(TypeBool type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.BOOL | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadBool();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadBool();");
            }
        }

        public void Visit(TypeBinary type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.BYTES | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadBytes();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadBytes();");
            }
        }

        public void Visit(TypeString type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.STRING | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadString();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadString();");
            }
        }

        public void Visit(TypeList type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            sw.WriteLine(prefix + "case (Zeze.ByteBuffer.LIST | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):");
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _state_ = _os_.BeginReadSegment();");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip collection.value typetag");
            sw.WriteLine(prefix + "        " + varname + " = new " + TypeName.GetName(type) + "();");
            sw.WriteLine(prefix + "        for (var _size_ = _os_.ReadInt(); _size_ > 0; --_size_)");
            sw.WriteLine(prefix + "        {");
            string vartmpname = Program.GenUniqVarName();
            valuetype.Accept(new Define(vartmpname, sw, prefix + "            "));
            valuetype.Accept(new Decode(vartmpname, -1, "_os_", sw, prefix + "            "));
            sw.WriteLine(prefix + "            " + varname + ".push(" + vartmpname + ");");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        _os_.EndReadSegment(_state_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    break;");
        }

        public void Visit(TypeSet type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            sw.WriteLine(prefix + "case (Zeze.ByteBuffer.SET | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):");
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _state_ = _os_.BeginReadSegment();");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip collection.value typetag");
            sw.WriteLine(prefix + "        " + varname + ".clear();");
            sw.WriteLine(prefix + "        for (var _size_ = _os_.ReadInt(); _size_ > 0; --_size_)");
            sw.WriteLine(prefix + "        {");
            string vartmpname = Program.GenUniqVarName();
            valuetype.Accept(new Define(vartmpname, sw, prefix + "            "));
            valuetype.Accept(new Decode(vartmpname, -1, "_os_", sw, prefix + "            "));
            sw.WriteLine(prefix + "            " + varname + ".add(" + vartmpname + ");");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        _os_.EndReadSegment(_state_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    break;");
        }

        public void Visit(TypeMap type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            Types.Type keytype = type.KeyType;
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + "case (Zeze.ByteBuffer.MAP | " + id + " << Zeze.ByteBuffer.TAG_SHIFT):");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _state_ = _os_.BeginReadSegment();");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip key typetag");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip value typetag");
            sw.WriteLine(prefix + "        " + varname + ".clear();");
            sw.WriteLine(prefix + "        for (var size = _os_.ReadInt(); size > 0; --size)");
            sw.WriteLine(prefix + "        {");
            string vartmpkey = Program.GenUniqVarName();
            string vartmpvalue = Program.GenUniqVarName();
            keytype.Accept(new Define(vartmpkey, sw, prefix + "            "));
            keytype.Accept(new Decode(vartmpkey, -1, "_os_", sw, prefix + "            "));
            valuetype.Accept(new Define(vartmpvalue, sw, prefix + "            "));
            valuetype.Accept(new Decode(vartmpvalue, -1, "_os_", sw, prefix + "            "));
            sw.WriteLine(prefix + "            " + varname + ".set(" + vartmpkey + ", " + vartmpvalue + ");");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        _os_.EndReadSegment(_state_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    break;");
        }

        public void Visit(TypeFloat type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.FLOAT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadFloat();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadFloat();");
            }
        }

        public void Visit(TypeShort type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.SHORT | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + varname + " = " + bufname + ".ReadShort();");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadShort();");
            }
        }

        public void Visit(TypeDynamic type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (Zeze.ByteBuffer.DYNAMIC | " + id + " << Zeze.ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + $"    {varname}.Decode({bufname});");
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
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                throw new Exception("invalid Variable.Id");
            }
        }
    }
}
