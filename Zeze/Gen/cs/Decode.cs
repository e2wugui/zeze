using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Decode : Visitor
    {
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void Decode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    int _t_ = _o_.ReadByte();");
            sw.WriteLine(prefix + "    int _i_ = _o_.ReadTagSize(_t_);");

            int lastId = 0;
            foreach (Variable v in bean.Variables)
            {
                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1)
                    {
                         sw.WriteLine(prefix + "    while (_t_ != 0 && _i_ < " + v.Id + ") {");
                         sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                         sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                         sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ")");
                }
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v.NameUpper1, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0)");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Decode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    int _t_ = _o_.ReadByte();");
            sw.WriteLine(prefix + "    int _i_ = _o_.ReadTagSize(_t_);");

            foreach (Variable v in bean.Variables)
            {
                if (v.Id > 0)
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ")");
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v.NamePrivate, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0)");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Decode(string varname, int id, string bufname, StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBool(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBool();");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadByte(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = (byte){bufname}.ReadLong();");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadShort(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = (short){bufname}.ReadLong();");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadInt(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadInt();");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadLong(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadLong();");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadFloat(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadFloat();");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadDouble(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadDouble();");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBinary(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBinary();");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadString(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadString();");
        }
 
        string DecodeElement(Types.Type type, string typeVar)
        {
            switch (type)
            {
                case TypeBool:
                    return bufname + ".ReadBool(" + typeVar + ')';
                case TypeByte:
                    return bufname + ".ReadByte(" + typeVar + ')';
                case TypeShort:
                    return bufname + ".ReadShort(" + typeVar + ')';
                case TypeInt:
                    return bufname + ".ReadInt(" + typeVar + ')';
                case TypeLong:
                    return bufname + ".ReadLong(" + typeVar + ')';
                case TypeFloat:
                    return bufname + ".ReadFloat(" + typeVar + ')';
                case TypeDouble:
                    return bufname + ".ReadDouble(" + typeVar + ')';
                case TypeBinary:
                    return bufname + ".ReadBinary(" + typeVar + ')';
                case TypeString:
                    return bufname + ".ReadString(" + typeVar + ')';
                case Bean:
                case BeanKey:
                case TypeDynamic:
                    return bufname + ".ReadBean(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }
 
        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            sw.WriteLine(prefix + "_x_.Clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "        _x_.Add(" + DecodeElement(vt, "_t_") + ");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(TypeList type)
        {
            DecodeCollection(type);
        }

        public void Visit(TypeSet type)
        {
            DecodeCollection(type);
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            sw.WriteLine(prefix + "_x_.Clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            sw.WriteLine(prefix + "        _x_.Add(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + varname + ", _t_);");
            else
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + varname + ", _t_);");
            else
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadDynamic(" + varname + ", _t_);");
            else
                throw new Exception("invalid variable.id");
        }
    }
}
