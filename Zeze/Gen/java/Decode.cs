using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Decode : Types.Visitor
    {
        private Variable var;
        private string tmpvarname;
        private int id;
        private string bufname;
        private System.IO.StreamWriter sw;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void Decode(ByteBuffer _os_) {");
            sw.WriteLine(prefix + "    for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count");
            sw.WriteLine(prefix + "        int _tagid_ = _os_.ReadInt();");
            sw.WriteLine(prefix + "        switch (_tagid_) {");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Decode(v, v.Id, "_os_", sw, prefix + "            "));
            }

            sw.WriteLine(prefix + "            default:");
            sw.WriteLine(prefix + "                ByteBuffer.SkipUnknownField(_tagid_, _os_);");
            sw.WriteLine(prefix + "                break;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void Decode(ByteBuffer _os_) {");
            sw.WriteLine(prefix + "    for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count");
            sw.WriteLine(prefix + "        int _tagid_ = _os_.ReadInt();");
            sw.WriteLine(prefix + "        switch (_tagid_) {");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Decode(v, v.Id, "_os_", sw, prefix + "            "));
            }

            sw.WriteLine(prefix + "            default:");
            sw.WriteLine(prefix + "                ByteBuffer.SkipUnknownField(_tagid_, _os_);");
            sw.WriteLine(prefix + "                break;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Decode(Variable var, int id, string bufname, System.IO.StreamWriter sw, string prefix)
        {
            this.var = var;
            this.tmpvarname = null;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }
        public Decode(string tmpvarname, int id, string bufname, System.IO.StreamWriter sw, string prefix)
        {
            this.var = null;
            this.tmpvarname = tmpvarname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }

        private string GetVarName()
        {
            if (null != var)
                return var.Bean.IsNormalBean
                    ? var.Getter : var.NamePrivate;
            return tmpvarname;
        }

        private string AssignText(string value)
        {
            if (null != var)
                return var.Bean.IsNormalBean
                    ? var.Setter(value) : $"{var.NamePrivate} = {value}";
            return $"{tmpvarname} = {value}";
        }

        void Visitor.Visit(Bean type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    {");
                sw.WriteLine(prefix + "        var _state_ = " + bufname + ".BeginReadSegment();");
                sw.WriteLine(prefix + "        " + GetVarName() + ".Decode(" + bufname + ");");
                sw.WriteLine(prefix + "        " + bufname + ".EndReadSegment(_state_);");
                sw.WriteLine(prefix + "    }");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + GetVarName() + ".Decode(" + bufname + ");");
            }
        }

        void Visitor.Visit(BeanKey type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.BEAN | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    {");
                sw.WriteLine(prefix + "        var _state_" + bufname + ".BeginReadSegment();");
                sw.WriteLine(prefix + "        " + GetVarName() + ".Decode(" + bufname + ");");
                sw.WriteLine(prefix + "        " + bufname + ".EndReadSegment(_state_);");
                sw.WriteLine(prefix + "    }");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + GetVarName() + ".Decode(" + bufname + ");");
            }
        }

        void Visitor.Visit(TypeByte type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.BYTE | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadByte()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadByte()") + ";");
            }
        }

        void Visitor.Visit(TypeDouble type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.DOUBLE | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadDouble()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadDouble()") + ";");
            }
        }

        void Visitor.Visit(TypeInt type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.INT | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadInt()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadInt()") + ";");
            }
        }

        void Visitor.Visit(TypeLong type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.LONG | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadLong()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadLong()") + ";");
            }
        }

        void Visitor.Visit(TypeBool type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.BOOL | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadBool()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBool()") + ";");
            }
        }

        void Visitor.Visit(TypeBinary type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.BYTES | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadBinary()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBinary()") + ";");
            }
        }

        void Visitor.Visit(TypeString type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.STRING | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadString()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadString()") + ";");
            }
        }

        private void DecodeCollection(TypeCollection type)
        {
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _state_ = _os_.BeginReadSegment();");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip collection.value typetag");
            sw.WriteLine(prefix + "        " + var.Getter + ".clear();");
            sw.WriteLine(prefix + "        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {");
            valuetype.Accept(new Define("_v_", sw, prefix + "            "));
            valuetype.Accept(new Decode("_v_", -1, "_os_", sw, prefix + "            "));
            sw.WriteLine(prefix + "            " + var.Getter + ".add(_v_);");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        _os_.EndReadSegment(_state_);");
            sw.WriteLine(prefix + "    }");
        }

        void Visitor.Visit(TypeList type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            sw.WriteLine(prefix + "case (ByteBuffer.LIST | " + id + " << ByteBuffer.TAG_SHIFT):");
            DecodeCollection(type);
            sw.WriteLine(prefix + "    break;");
        }

        void Visitor.Visit(TypeSet type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            sw.WriteLine(prefix + "case (ByteBuffer.SET | " + id + " << ByteBuffer.TAG_SHIFT):");
            DecodeCollection(type);
            sw.WriteLine(prefix + "    break;");
        }

        void Visitor.Visit(TypeMap type)
        {
            if (id < 0)
                throw new Exception("invalid variable.id");

            Types.Type keytype = type.KeyType;
            Types.Type valuetype = type.ValueType;

            sw.WriteLine(prefix + "case (ByteBuffer.MAP | " + id + " << ByteBuffer.TAG_SHIFT):");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _state_ = _os_.BeginReadSegment();");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip key typetag");
            sw.WriteLine(prefix + "        _os_.ReadInt(); // skip value typetag");
            sw.WriteLine(prefix + "        " + var.Getter + ".clear();");
            sw.WriteLine(prefix + "        for (int size = _os_.ReadInt(); size > 0; --size) {");
            keytype.Accept(new Define("_k_", sw, prefix + "            "));
            keytype.Accept(new Decode("_k_", -1, "_os_", sw, prefix + "            "));
            valuetype.Accept(new Define("_v_", sw, prefix + "            "));
            valuetype.Accept(new Decode("_v_", -1, "_os_", sw, prefix + "            "));
            sw.WriteLine(prefix + "            " + var.Getter + ".put(_k_, _v_);");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        _os_.EndReadSegment(_state_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    break;");
        }

        void Visitor.Visit(TypeFloat type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.FLOAT | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadFloat()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadFloat()") + ";");
            }
        }

        void Visitor.Visit(TypeShort type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.SHORT | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + "    " + AssignText($"{bufname}.ReadShort()") + ";");
                sw.WriteLine(prefix + "    break;");
            }
            else
            {
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadShort()") + ";");
            }
        }

        void Visitor.Visit(TypeDynamic type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "case (ByteBuffer.DYNAMIC | " + id + " << ByteBuffer.TAG_SHIFT): ");
                sw.WriteLine(prefix + $"    {GetVarName()}.Decode({bufname});");
                /*
                sw.WriteLine(prefix + "    switch (" + bufname + ".ReadLong8())");
                sw.WriteLine(prefix + "    {");
                foreach (Bean real in type.RealBeans)
                {
                    string realName = TypeName.GetName(real);
                    sw.WriteLine(prefix + "        case " + realName + ".TYPEID:");
                    sw.WriteLine(prefix + "            {");
                    sw.WriteLine(prefix + "                " + bufname + ".BeginReadSegment(out var _state_);");
                    sw.WriteLine(prefix + "                " + varname + " = new " + realName + "();");
                    sw.WriteLine(prefix + "                " + varname + ".Decode(" + bufname + ");");
                    sw.WriteLine(prefix + "                " + bufname + ".EndReadSegment(_state_);");
                    sw.WriteLine(prefix + "            }");
                    sw.WriteLine(prefix + "            break;");
                }
                sw.WriteLine(prefix + "        case Zeze.Transaction.EmptyBean.TYPEID:");
                sw.WriteLine(prefix + "            " + varname + " = new Zeze.Transaction.EmptyBean();");
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
                throw new Exception("invalie Variable.Id");
            }
        }
    }
}
