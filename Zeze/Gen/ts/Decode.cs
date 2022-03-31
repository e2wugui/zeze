using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
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
            sw.WriteLine(prefix + "public Decode(_o_: Zeze.ByteBuffer) {");
            sw.WriteLine(prefix + "    var _t_ = _o_.ReadByte();");
            sw.WriteLine(prefix + "    var _i_ = _o_.ReadTagSize(_t_);");

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
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ") {");
                }
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0) {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Decode(_o_: Zeze.ByteBuffer) {");
            sw.WriteLine(prefix + "    var _t_ = _o_.ReadByte();");
            sw.WriteLine(prefix + "    var _i_ = _o_.ReadTagSize(_t_);");

            foreach (Variable v in bean.Variables)
            {
                if (v.Id > 0)
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ") {");
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0) {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
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
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBoolT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBool();");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadIntT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadInt();");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadIntT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadInt();");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadIntT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadInt();");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadLongT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadLong();");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadFloatT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadFloat();");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadDoubleT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadDouble();");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBytesT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadBytes();");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadStringT(_t_);");
            else
                sw.WriteLine(prefix + $"{varname} = {bufname}.ReadString();");
        }

        private string DecodeElement(Types.Type type, string typeVar)
        {
            switch (type)
            {
                case TypeBool:
                    return bufname + ".ReadBoolT(" + typeVar + ')';
                case TypeByte:
                    return bufname + ".ReadIntT(" + typeVar + ')';
                case TypeShort:
                    return bufname + ".ReadIntT(" + typeVar + ')';
                case TypeInt:
                    return bufname + ".ReadIntT(" + typeVar + ')';
                case TypeLong:
                    return bufname + ".ReadLongT(" + typeVar + ')';
                case TypeFloat:
                    return bufname + ".ReadFloatT(" + typeVar + ')';
                case TypeDouble:
                    return bufname + ".ReadDoubleT(" + typeVar + ')';
                case TypeBinary:
                    return bufname + ".ReadBytesT(" + typeVar + ')';
                case TypeString:
                    return bufname + ".ReadStringT(" + typeVar + ')';
                case Bean:
                case BeanKey:
                case TypeDynamic:
                    return bufname + ".ReadBean(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public void Visit(TypeList type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x" + id + "_ = new " + TypeName.GetName(type) + "();");
            sw.WriteLine(prefix + varname + " = _x" + id + "_;");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    for (var _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "        _x" + id + "_.push(" + DecodeElement(vt, "_t_") + ");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(TypeSet type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x" + id + "_ = " + varname + ';');
            sw.WriteLine(prefix + "_x" + id + "_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    for (var _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "        _x" + id + "_.add(" + DecodeElement(vt, "_t_") + ");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x" + id + "_ = " + varname + ';');
            sw.WriteLine(prefix + "_x" + id + "_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    var _s_ = (_t_ = " + bufname + ".ReadByte()) >> Zeze.ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (var _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--) {");
            sw.WriteLine(prefix + "        var _k" + id + "_ = " + DecodeElement(kt, "_s_") + ';');
            sw.WriteLine(prefix + "        var _v" + id + "_ = " + DecodeElement(vt, "_t_") + ';');
            sw.WriteLine(prefix + "        _x" + id + "_.set(_k" + id + "_, _v" + id + "_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
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
