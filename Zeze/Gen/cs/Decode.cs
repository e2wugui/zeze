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
        readonly string varUpperName1;
        readonly string typeVarName;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool varNameUpper = true)
        {
            sw.WriteLine(prefix + "public override void Decode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    int _t_ = _o_.ReadByte();");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = _o_.ReadTagSize(_t_);");
            else
                sw.WriteLine(prefix + "    _o_.ReadTagSize(_t_);");

            int lastId = 0;
            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1)
                    {
                        sw.WriteLine(prefix + "    while ((_t_ & 0xff) > 1 && _i_ < " + v.Id + ")");
                        sw.WriteLine(prefix + "    {");
                        sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                        sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                        sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ")");
                }
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(varNameUpper ? v.NameUpper1 : v.Name, v.Id, "_o_", sw, prefix + "        ", v.NameUpper1, null));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    if (v.Initial.Length > 0)
                    {
                        sw.WriteLine(prefix + "    }");
                        sw.WriteLine(prefix + "    else");
                        sw.WriteLine(prefix + "        " + Initial(v) + ";");
                    }
                    else
                        sw.WriteLine(prefix + "    }");
                }
                else
                    sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0)");
            sw.WriteLine(prefix + "    {");
            if (bean.Base != "")
            {
                sw.WriteLine(prefix + "        if (_t_ == 1)");
                sw.WriteLine(prefix + "        {");
                sw.WriteLine(prefix + "            base.Decode(_o_);");
                sw.WriteLine(prefix + "            return;");
                sw.WriteLine(prefix + "        }");
            }
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
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = _o_.ReadTagSize(_t_);");
            else
                sw.WriteLine(prefix + "    _o_.ReadTagSize(_t_);");

            int lastId = 0;
            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1)
                    {
                        sw.WriteLine(prefix + "    while ((_t_ & 0xff) > 1 && _i_ < " + v.Id + ")");
                        sw.WriteLine(prefix + "    {");
                        sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                        sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                        sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ")");
                }
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v.NamePrivate, v.Id, "_o_", sw, prefix + "        ", v.NameUpper1, null));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    if (v.Initial.Length > 0)
                    {
                        sw.WriteLine(prefix + "    }");
                        sw.WriteLine(prefix + "    else");
                        sw.WriteLine(prefix + "        " + Initial(v) + ";");
                    }
                    else
                        sw.WriteLine(prefix + "    }");
                }
                else
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

        static string Initial(Variable var)
        {
            var type = var.VariableType;
            switch (type)
            {
                case TypeBool:
                    return var.Bean.IsNormalBean ? $"{var.NameUpper1} = false" : $"{var.NamePrivate} = false";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                case TypeFloat:
                case TypeDouble:
                    return var.Bean.IsNormalBean ? $"{var.NameUpper1} = 0" : $"{var.NamePrivate} = 0";
                case TypeString:
                    return var.Bean.IsNormalBean ? $"{var.NameUpper1} = \"\"" : $"{var.NamePrivate} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    return var.Bean.IsNormalBean
                        ? $"{var.NameUpper1} = new {TypeName.GetName(type)}()"
                        : $"{var.NamePrivate} = new {TypeName.GetName(type)}()";
                default:
                    throw new Exception("unsupported initial type: " + var.VariableType);
            }
        }

        public Decode(string varname, int id, string bufname, StreamWriter sw, string prefix, string varUpperName1, string typeVarName)
        {
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.varUpperName1 = varUpperName1;
            this.typeVarName = typeVarName ?? "_t_";
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
                    return bufname + ".ReadBean(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeDynamic:
                    return bufname + ".ReadDynamic(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeVector2:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadVector2({bufname}, {typeVar})"
                        : $"{bufname}.ReadVector2({typeVar})";
                case TypeVector3:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadVector3({bufname}, {typeVar})"
                        : $"{bufname}.ReadVector3({typeVar})";
                case TypeVector4:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadVector4({bufname}, {typeVar})"
                        : $"{bufname}.ReadVector4({typeVar})";
                case TypeQuaternion:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadQuaternion({bufname}, {typeVar})"
                        : $"{bufname}.ReadQuaternion({typeVar})";
                case TypeVector2Int:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadVector2Int({bufname}, {typeVar})"
                        : $"{bufname}.ReadVector2Int({typeVar})";
                case TypeVector3Int:
                    return Project.MakingInstance.IsUnity
                        ? $"Zeze.Util.UnityVectorHelper.ReadVector3Int({bufname}, {typeVar})"
                        : $"{bufname}.ReadVector3Int({typeVar})";

                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public static bool IsOldStyleEncodeDecodeType(Types.Type type)
        {
            if (type is TypeDynamic)
                return true;

            if (!Project.MakingInstance.Platform.StartsWith("conf+cs"))
                return false;

            switch (type)
            {
                case TypeVector2:
                case TypeVector3:
                case TypeVector4:
                case TypeVector2Int:
                case TypeVector3Int:
                case TypeQuaternion:
                    return false;
            }
            return false;
        }

        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            if (type is TypeArray && vt is TypeByte)
            {
                sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.BYTES)");
                sw.WriteLine(prefix + $"    {varname} = {bufname}.ReadBytes();");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + $"    {bufname}.SkipUnknownFieldOrThrow(_t_, \"byte[]\");");
                return;
            }
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            bool isArray = type is TypeArray || type is TypeList list && list.FixSize >= 0;
            if (!isArray)
                sw.WriteLine(prefix + "_x_.Clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ")");
            sw.WriteLine(prefix + "{");
            if (isArray)
            {
                sw.WriteLine(prefix + "    int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte());");
                sw.WriteLine(prefix + "    if (_x_ == null || _x_.Length != _n_)");
                sw.WriteLine(prefix + "        " + varname + " = _x_ = new " + confcs.TypeName.GetName(vt) + "[_n_];");
                sw.WriteLine(prefix + "    for (int _j_ = 0; _j_ < _n_; _j_++)");
                sw.WriteLine(prefix + "    {");
                if (IsOldStyleEncodeDecodeType(vt))
                {
                    vt.Accept(new Define("_e_", sw, prefix + "        "));
                    vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        ", varUpperName1, "_t_"));
                    sw.WriteLine(prefix + "        _x_[_j_] = _e_;");
                }
                else
                    sw.WriteLine(prefix + "        _x_[_j_] = " + DecodeElement(vt, "_t_") + ';');
            }
            else
            {
                sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
                sw.WriteLine(prefix + "    {");
                if (IsOldStyleEncodeDecodeType(vt))
                {
                    vt.Accept(new Define("_e_", sw, prefix + "        "));
                    vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        ", varUpperName1, "_t_"));
                    sw.WriteLine(prefix + "        _x_.Add(_e_);");
                }
                else
                    sw.WriteLine(prefix + "        _x_.Add(" + DecodeElement(vt, "_t_") + ");");
            }
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Collection\");");
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
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        ", varUpperName1, "_s_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        ", varUpperName1, "_t_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
            sw.WriteLine($"{prefix}        _x_.Add(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
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
            if (Project.MakingInstance.Platform.StartsWith("conf+cs"))
            {
                var tmpName = id > 0 ? "_x_" : "_y_";
                sw.WriteLine($"{prefix}if (({typeVarName} & ByteBuffer.TAG_MASK) == ByteBuffer.DYNAMIC)");
                sw.WriteLine($"{prefix}{{");
                sw.WriteLine($"{prefix}    var {tmpName} = CreateBeanFromSpecialTypeId_{type.Variable.Id}({bufname}.ReadLong());");
                sw.WriteLine($"{prefix}    {tmpName}.Decode({bufname});");
                sw.WriteLine($"{prefix}    {varname} = {tmpName};");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    {bufname}.SkipUnknownFieldOrThrow(_t_, \"DynamicBean\");");
            }
            else if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadDynamic(" + varname + ", _t_);");
            else
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeVector2 type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeVector2Int type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeVector3 type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeVector3Int type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadInt4();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadInt4();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeVector4 type)
        {
            if (Project.MakingInstance.IsUnity)
            {
                sw.WriteLine($"{prefix}{varname}.x = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.y = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.z = {bufname}.ReadFloat();");
                sw.WriteLine($"{prefix}{varname}.w = {bufname}.ReadFloat();");
            }
            else
            {
                sw.WriteLine($"{prefix}{varname}.Decode({bufname});");
            }
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}{varname} = decimal.Parse({bufname}.ReadString());");
        }
    }
}
