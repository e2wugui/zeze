using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Decode : Visitor
    {
        readonly Variable var;
        readonly string tmpvarname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly string typeVarName;
        readonly bool withUnknown;

        string Getter => var != null ? var.Getter : tmpvarname;
        string NamePrivate => var != null ? var.NamePrivate : tmpvarname;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool withUnknown)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decode(IByteBuffer _o_) {");
            if (withUnknown)
                sw.WriteLine(prefix + "    ByteBuffer _u_ = null;");
            sw.WriteLine(prefix + "    int _t_ = _o_.ReadByte();");
            if (bean.VariablesIdOrder.Count > 0 || withUnknown)
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
                        sw.WriteLine(prefix + "    while ((_t_ & 0xff) > 1 && _i_ < " + v.Id + ") {");
                        if (withUnknown)
                            sw.WriteLine(prefix + "        _u_ = _o_.readUnknownField(_i_, _t_, _u_);");
                        else
                            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                        sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                        sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ") {");
                }
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, prefix + "        ", withUnknown));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    WriteMissingField(v, sw, prefix, false);
                }
                else
                    sw.WriteLine(prefix + "    }");
            }

            if (bean.Base == "")
            {
                if (withUnknown)
                {
                    sw.WriteLine(prefix + "    //noinspection ConstantValue");
                    sw.WriteLine(prefix + "    _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);");
                }
                else
                    sw.WriteLine(prefix + "    _o_.skipAllUnknownFields(_t_);");
            }
            else
            {
                sw.WriteLine(prefix + "    while (_t_ != 0) {");
                sw.WriteLine(prefix + "        if (_t_ == 1) {");
                sw.WriteLine(prefix + "            base.decode(_o_);");
                sw.WriteLine(prefix + "            return;");
                sw.WriteLine(prefix + "        }");
                sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                sw.WriteLine(prefix + "    }");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
                        sw.WriteLine(prefix + "    while ((_t_ & 0xff) > 1 && _i_ < " + v.Id + ") {");
                        sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                        sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                        sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ == " + v.Id + ") {");
                }
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, prefix + "        ", false));
                if (v.Id > 0)
                {
                    sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                    WriteMissingField(v, sw, prefix, false);
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

        internal static void WriteMissingField(Variable var, StreamWriter sw, string prefix, bool isData)
        {
            var type = var.VariableType;
            if (isData && type is TypeGTable)
            {
                // GTable has no Data field or Data encoding.
                sw.WriteLine(prefix + "    }");
                return;
            }

            sw.WriteLine(prefix + "    } else");
            if (isData && type is TypeSortedMap)
                sw.WriteLine($"{prefix}        {var.NamePrivate} = new java.util.TreeMap<>();");
            else if (type is TypeCollection or TypeMap or TypeSortedMap or TypeGTable)
                sw.WriteLine($"{prefix}        {var.NamePrivate}.clear();");
            else if (type is TypeDynamic)
                sw.WriteLine($"{prefix}        {var.NamePrivate}.reset();");
            else if (type is Bean child)
                sw.WriteLine($"{prefix}        {RestoreOmittedBean.Missing(var, child, isData)};");
            else
            {
                string FieldTypeName() => isData ? javadata.TypeName.GetName(type) : TypeName.GetName(type);
                var value = type switch
                {
                    TypeBool => "false",
                    TypeByte => "(byte)0",
                    TypeShort => "(short)0",
                    TypeInt or TypeLong or TypeFloat or TypeDouble => "0",
                    TypeString => "\"\"",
                    TypeBinary => "Zeze.Net.Binary.Empty",
                    // A missing child tag left the freshly constructed child as-is in legacy readers.
                    BeanKey when var.Initial.Length == 0 || type is not ExternalBeanKey => $"new {FieldTypeName()}()",
                    BeanKey or TypeVector2 or TypeVector2Int or TypeVector3 or TypeVector3Int
                        or TypeVector4 or TypeQuaternion => $"{FieldTypeName()}.ZERO",
                    // Decimal is always written, even for zero. Older schemas may lack the field.
                    TypeDecimal when var.Initial.Length == 0 => "java.math.BigDecimal.ZERO",
                    TypeDecimal => $"new java.math.BigDecimal(\"{var.Initial}\", java.math.MathContext.DECIMAL128)",
                    _ => throw new Exception("unsupported missing field type: " + type),
                };
                var statement = !isData && var.Bean.IsNormalBean ? var.Setter(value) : $"{var.NamePrivate} = {value}";
                sw.WriteLine($"{prefix}        {statement};");
            }
        }

        public Decode(Variable var, int id, string bufname, StreamWriter sw, string prefix, bool withUnknown)
        {
            this.var = var;
            this.tmpvarname = null;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = "_t_";
            this.withUnknown = withUnknown;
        }

        public Decode(string tmpvarname, int id, string bufname, StreamWriter sw, string prefix, bool withUnknown, string typeVarName = null)
        {
            this.var = null;
            this.tmpvarname = tmpvarname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = typeVarName ?? "_t_";
            this.withUnknown = withUnknown;
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
                    return bufname + ".ReadDynamic(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeVector2:
                    return bufname + ".ReadVector2(" + typeVar + ')';
                case TypeVector2Int:
                    return bufname + ".ReadVector2Int(" + typeVar + ')';
                case TypeVector3:
                    return bufname + ".ReadVector3(" + typeVar + ')';
                case TypeVector3Int:
                    return bufname + ".ReadVector3Int(" + typeVar + ')';
                case TypeVector4:
                    return bufname + ".ReadVector4(" + typeVar + ')';
                case TypeQuaternion:
                    return bufname + ".ReadQuaternion(" + typeVar + ')';
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public static bool IsOldStyleEncodeDecodeType(Types.Type type)
        {
            switch (type)
            {
                case TypeDynamic:
                    return true;
            }
            return false;
        }

        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.Write(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            if (IsOldStyleEncodeDecodeType(vt))
            {
                sw.WriteLine(" {");
                vt.Accept(new Define("_e_", sw, prefix + "        "));
                vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        ", withUnknown));
                sw.WriteLine($"{prefix}        _x_.add(_e_);");
                sw.WriteLine($"{prefix}    }}");
            }
            else
            {
                sw.WriteLine();
                sw.WriteLine(prefix + "        _x_.add(" + DecodeElement(vt, "_t_") + ");");
            }
            sw.WriteLine($"{prefix}}} else");
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
            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUIntPositive(); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        ", withUnknown, "_s_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        ", withUnknown));
            }
            else
            {
                sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
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
            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUIntPositive(); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        ", withUnknown, "_s_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        ", withUnknown));
            }
            else
            {
                sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
            sw.WriteLine(prefix + "        _x_.put(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + NamePrivate + ", _t_);");
            else
                sw.WriteLine(prefix + NamePrivate + ".decode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadBean(new {TypeName.GetName(type)}({var?.Initial}), {typeVarName})") + ';');
            else
                sw.WriteLine(prefix + Getter + ".decode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + bufname + ".ReadDynamic(" + NamePrivate + ", " + typeVarName + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadQuaternion({typeVarName})") + ';');
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadVector2({typeVarName})") + ';');
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadVector2Int({typeVarName})") + ';');
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadVector3({typeVarName})") + ';');
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadVector3Int({typeVarName})") + ';');
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufname}.ReadVector4({typeVarName})") + ';');
        }

        public void Visit(TypeDecimal type)
        {
            // 不带 MathContext：与 LogDecimal.decode 一致保持 encode/decode 双射（FND3-06）。
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufname}.ReadString(_t_))") + ';');
            else
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufname}.ReadString())") + ';');
        }

        public void Visit(TypeGTable type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.RowKeyType;

            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUIntPositive(); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        ", withUnknown, "_s_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            sw.WriteLine(prefix + $"        var _v_ = _o_.ReadBean({var.NamePrivate}.getPMap2().createValue(), _t_);");
            sw.WriteLine(prefix + "        _x_.getPMap2().put(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }
    }
}
