using System;
using System.IO;
using Zeze.Gen.Types;
using Zeze.Serialize;

namespace Zeze.Gen.cs
{
    public class Decode : Visitor
    {
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool varNameUpper = true)
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
                         sw.WriteLine(prefix + "    while (_t_ != 0 && _i_ < " + v.Id + ")");
                         sw.WriteLine(prefix + "    {");
                         sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                         sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                         sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ")");
                }
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(varNameUpper ? v.NameUpper1 : v.Name, v.Id, "_o_", sw, prefix + "        "));
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
                case TypeVector2:
                    return $"{bufname}.ReadVector2({typeVar})";
                case TypeVector3:
                    return $"{bufname}.ReadVector3({typeVar})";
                case TypeVector4:
                    return $"{bufname}.ReadVector4({typeVar})";
                case TypeQuaternion:
                    return $"{bufname}.ReadQuaternion({typeVar})";
                case TypeVector2Int:
                    return $"{bufname}.ReadVector2Int({typeVar})";
                case TypeVector3Int:
                    return $"{bufname}.ReadVector3Int({typeVar})";

                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }
 
        public static bool IsUnityType(Types.Type type)
        {
            if (!Project.MakingInstance.Platform.Equals("conf+cs"))
                return false;

            switch (type)
            {
                case TypeVector2:
                case TypeVector3:
                case TypeVector4:
                case TypeVector2Int:
                case TypeVector3Int:
                case TypeQuaternion:
                    return true;
            }
            return false;
        }

        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            bool isFixSizeList = type is TypeList list && list.FixSize >= 0;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            if (false == isFixSizeList)
                sw.WriteLine(prefix + "_x_.Clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "    {");
            if (IsUnityType(vt))
            {
                vt.Accept(new Define("_e_", sw, prefix + "        "));
                vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        "));
                if (isFixSizeList)
                {
                    sw.WriteLine($"{prefix}        _x_[_x_.Length - _n_] = _e_;");
                }
                else
                {
                    sw.WriteLine($"{prefix}        _x_.Add(_e_);");
                }
            }
            else
            {
                if (isFixSizeList)
                {
                    sw.WriteLine($"{prefix}        _x_[_x_.Length - _n_] = _e_;");
                }
                else
                {
                    sw.WriteLine(prefix + "        _x_.Add(" + DecodeElement(vt, "_t_") + ");");
                }
            }
            sw.WriteLine(prefix + "    }");
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
            if (IsUnityType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsUnityType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
            sw.WriteLine($"{prefix}        _x_.Add(_k_, _v_);");
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

        public void Visit(TypeQuaternion type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
        }

        public void Visit(TypeVector2 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
            }
        }

        public void Visit(TypeVector2Int type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
            }
        }

        public void Visit(TypeVector3 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
            }
        }

        public void Visit(TypeVector3Int type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadInt4();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadInt4();");
            }
        }

        public void Visit(TypeVector4 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
        }
    }
}
