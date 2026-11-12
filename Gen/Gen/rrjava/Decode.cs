using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class Decode : Visitor
    {
        readonly Variable var;
        readonly string tmpvarname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decode(IByteBuffer _o_) {");
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
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    sw.WriteLine(prefix + "    } else {");
                    WriteMissingField(v, sw, prefix + "        ");
                    sw.WriteLine(prefix + "    }");
                }
                else
                    sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0) {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        private static void WriteMissingField(Variable variable, StreamWriter sw, string prefix)
        {
            var field = variable.NamePrivate;
            var type = variable.VariableType;
            // Encode omits zero, empty values and empty nested bean bodies, independent of constructor defaults.
            switch (type)
            {
                case TypeBool:
                    sw.WriteLine($"{prefix}{field} = false;");
                    break;
                case TypeByte or TypeShort or TypeInt or TypeLong or TypeFloat or TypeDouble:
                    sw.WriteLine($"{prefix}{field} = 0;");
                    break;
                case TypeString:
                    sw.WriteLine($"{prefix}{field} = \"\";");
                    break;
                case TypeBinary:
                    sw.WriteLine($"{prefix}{field} = Zeze.Net.Binary.Empty;");
                    break;
                case TypeVector2 or TypeVector2Int or TypeVector3 or TypeVector3Int or TypeVector4 or TypeQuaternion:
                    sw.WriteLine($"{prefix}{field} = {TypeName.GetName(type)}.ZERO;");
                    break;
                case TypeDecimal:
                    // The encoder always writes decimal values, including zero.
                    new Construct(sw, variable, prefix).Visit((TypeDecimal)type);
                    break;
                case TypeCollection or TypeMap or TypeSortedMap or TypeDynamic:
                    sw.WriteLine($"{prefix}{field}.clear();");
                    break;
                case Bean child:
                    sw.WriteLine($"{prefix}{java.Decode.MissingBean(variable, child, false)};");
                    break;
                case BeanKey:
                    var key = java.Decode.MissingBeanKey(variable, TypeName.GetName(type));
                    sw.WriteLine($"{prefix}{field} = {key};");
                    break;
            }
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decode(IByteBuffer _o_) {");
            sw.WriteLine(prefix + "    int _t_ = _o_.ReadByte();");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = _o_.ReadTagSize(_t_);");
            else
                sw.WriteLine(prefix + "    _o_.ReadTagSize(_t_);");

            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ") {");
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, prefix + "        "));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    if (v.Initial.Length > 0)
                    {
                        sw.WriteLine(prefix + "    } else");
                        sw.WriteLine(prefix + "        " + Initial(v) + ";");
                    }
                    else
                        sw.WriteLine(prefix + "    }");
                }
                else
                    sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    while (_t_ != 0) {");
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
                    return var.Bean.IsNormalBean ? var.Setter("false") : $"{var.NamePrivate} = false";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                case TypeFloat:
                case TypeDouble:
                    return var.Bean.IsNormalBean ? var.Setter("0") : $"{var.NamePrivate} = 0";
                case TypeString:
                    return var.Bean.IsNormalBean ? var.Setter("") : $"{var.NamePrivate} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    return var.Bean.IsNormalBean
                        ? var.Setter($"{TypeName.GetName(type)}.ZERO")
                        : $"{var.NamePrivate} = {TypeName.GetName(type)}.ZERO";
                default:
                    throw new Exception("unsupported initial type: " + var.VariableType);
            }
        }

        public Decode(Variable var, int id, string bufname, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.tmpvarname = null;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }
 
        public Decode(string tmpvarname, int id, string bufname, StreamWriter sw, string prefix)
        {
            this.var = null;
            this.tmpvarname = tmpvarname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }

        string GetVarName()
        {
            if (var != null)
                return var.Bean.IsNormalBean ? var.Getter : var.NamePrivate;
            return tmpvarname;
        }

        string AssignText(string value)
        {
            if (var != null)
                return var.Bean.IsNormalBean ? var.Setter(value) : $"{var.NamePrivate} = {value}";
            return $"{tmpvarname} = {value}";
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBool(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBool()") + ';');
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadByte(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"(byte){bufname}.ReadLong()") + ';');
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadShort(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"(short){bufname}.ReadLong()") + ';');
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadInt(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadInt()") + ';');
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadLong(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadLong()") + ';');
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadFloat(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadFloat()") + ';');
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadDouble(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadDouble()") + ';');
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBinary(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBinary()") + ';');
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadString(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadString()") + ';');
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
                    return $"newDynamicBean_{var.NameUpper1}().decodeTagged({bufname}, {typeVar})";
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + var.Getter + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(prefix + "        _x_.add(" + DecodeElement(vt, "_t_") + ");");
            sw.WriteLine(prefix + "} else");
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
            sw.WriteLine(prefix + "var _x_ = " + var.Getter + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--) {");
            sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            sw.WriteLine(prefix + "        _x_.put(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }

        public void Visit(TypeSortedMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + var.Getter + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--) {");
            sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            sw.WriteLine(prefix + "        _x_.put(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + GetVarName() + ", _t_);");
            else
                sw.WriteLine(prefix + GetVarName() + ".decode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBean(new {TypeName.GetName(type)}({var?.Initial}), _t_)") + ';');
            else
                sw.WriteLine(prefix + GetVarName() + ".decode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
                sw.WriteLine(prefix + GetVarName() + $".decodeTagged({bufname}, _t_);");
            else
                throw new Exception("invalid variable.id");
        }

        private void DecodeVector(string typeName)
        {
            var tag = id > 0 ? "_t_" : "";
            sw.WriteLine(prefix + AssignText($"{bufname}.Read{typeName}({tag})") + ';');
        }

        public void Visit(TypeQuaternion type)
        {
            DecodeVector("Quaternion");
        }

        public void Visit(TypeVector2 type)
        {
            DecodeVector("Vector2");
        }

        public void Visit(TypeVector2Int type)
        {
            DecodeVector("Vector2Int");
        }

        public void Visit(TypeVector3 type)
        {
            DecodeVector("Vector3");
        }

        public void Visit(TypeVector3Int type)
        {
            DecodeVector("Vector3Int");
        }

        public void Visit(TypeVector4 type)
        {
            DecodeVector("Vector4");
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufname}.ReadString(_t_), java.math.MathContext.DECIMAL128)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufname}.ReadString(), java.math.MathContext.DECIMAL128)") + ';');
        }

        public void Visit(TypeGTable type)
        {
            //throw new NotImplementedException();
        }
    }
}
