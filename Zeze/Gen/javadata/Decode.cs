using System;
using System.IO;
using Zeze.Gen.java;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Decode : Visitor
    {
        readonly Variable var;
        readonly string tmpVarName;
        readonly int id;
        readonly string bufName;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly string typeVarName;

        string NamePrivate => var != null ? var.NamePrivate : tmpVarName;

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
            if (bean.Base != "")
            {
                sw.WriteLine(prefix + "        if (_t_ == 1) {");
                sw.WriteLine(prefix + "            base.decode(_o_);");
                sw.WriteLine(prefix + "            return;");
                sw.WriteLine(prefix + "        }");
            }
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
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
                    return $"{var.NamePrivate} = false";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                case TypeFloat:
                case TypeDouble:
                    return $"{var.NamePrivate} = 0";
                case TypeString:
                    return $"{var.NamePrivate} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    return $"{var.NamePrivate} = {TypeName.GetName(type)}.ZERO";
                default:
                    throw new Exception("unsupported initial type: " + var.VariableType);
            }
        }

        public Decode(Variable var, int id, string bufName, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.tmpVarName = null;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = "_t_";
        }

        public Decode(string tmpVarName, int id, string bufName, StreamWriter sw, string prefix, string typeVarName = null)
        {
            this.var = null;
            this.tmpVarName = tmpVarName;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = typeVarName ?? "_t_";
        }

        string AssignText(string value)
        {
            if (var != null)
                return $"{var.NamePrivate} = {value}";
            return $"{tmpVarName} = {value}";
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadBool(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadBool()") + ';');
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadByte(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"(byte){bufName}.ReadLong()") + ';');
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadShort(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"(short){bufName}.ReadLong()") + ';');
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadInt(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadInt()") + ';');
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadLong(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadLong()") + ';');
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadFloat(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadFloat()") + ';');
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadDouble(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadDouble()") + ';');
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadBinary(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadBinary()") + ';');
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadString(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufName}.ReadString()") + ';');
        }

        string DecodeElement(Types.Type type, string typeVar)
        {
            switch (type)
            {
                case TypeBool:
                    return bufName + ".ReadBool(" + typeVar + ')';
                case TypeByte:
                    return bufName + ".ReadByte(" + typeVar + ')';
                case TypeShort:
                    return bufName + ".ReadShort(" + typeVar + ')';
                case TypeInt:
                    return bufName + ".ReadInt(" + typeVar + ')';
                case TypeLong:
                    return bufName + ".ReadLong(" + typeVar + ')';
                case TypeFloat:
                    return bufName + ".ReadFloat(" + typeVar + ')';
                case TypeDouble:
                    return bufName + ".ReadDouble(" + typeVar + ')';
                case TypeBinary:
                    return bufName + ".ReadBinary(" + typeVar + ')';
                case TypeString:
                    return bufName + ".ReadString(" + typeVar + ')';
                case Bean:
                case BeanKey:
                    return bufName + ".ReadBean(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeDynamic:
                    return bufName + ".ReadDynamic(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeVector2:
                    return bufName + ".ReadVector2(" + typeVar + ')';
                case TypeVector2Int:
                    return bufName + ".ReadVector2Int(" + typeVar + ')';
                case TypeVector3:
                    return bufName + ".ReadVector3(" + typeVar + ')';
                case TypeVector3Int:
                    return bufName + ".ReadVector3Int(" + typeVar + ')';
                case TypeVector4:
                    return bufName + ".ReadVector4(" + typeVar + ')';
                case TypeQuaternion:
                    return bufName + ".ReadQuaternion(" + typeVar + ')';
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

        public void Visit(TypeList type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            if (!string.IsNullOrEmpty(type.Variable.JavaType))
            {
                sw.WriteLine(prefix + "    _t_ = " + bufName + ".ReadByte();");
                sw.WriteLine(prefix + "    int _n_ = " + bufName + ".ReadTagSize(_t_);");
                sw.WriteLine(prefix + "    if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type.ValueType) + ")");
                sw.WriteLine(prefix + "        _x_.decode(" + bufName + ", _n_);");
                sw.WriteLine(prefix + "    else {");
                sw.WriteLine(prefix + "        for (; _n_ > 0; _n_--)");
                sw.WriteLine(prefix + "            _x_.add(" + DecodeElement(vt, "_t_") + ");");
                sw.WriteLine(prefix + "    }");
            }
            else
            {
                sw.Write(prefix + "    for (int _n_ = " + bufName + ".ReadTagSize(_t_ = " + bufName + ".ReadByte()); _n_ > 0; _n_--)");
                if (IsOldStyleEncodeDecodeType(vt))
                {
                    sw.WriteLine(" {");
                    vt.Accept(new Define("_e_", sw, prefix + "        "));
                    vt.Accept(new Decode("_e_", 0, bufName, sw, prefix + "        "));
                    sw.WriteLine($"{prefix}        _x_.add(_e_);");
                    sw.WriteLine($"{prefix}    }}");
                }
                else
                {
                    sw.WriteLine();
                    sw.WriteLine(prefix + "        _x_.add(" + DecodeElement(vt, "_t_") + ");");
                }
            }
            sw.WriteLine($"{prefix}}} else");
            sw.WriteLine(prefix + "    " + bufName + ".SkipUnknownFieldOrThrow(_t_, \"Collection\");");
        }

        public void Visit(TypeSet type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + var.NamePrivate + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & ByteBuffer.TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.Write(prefix + "    for (int _n_ = " + bufName + ".ReadTagSize(_t_ = " + bufName + ".ReadByte()); _n_ > 0; _n_--)");
            if (IsOldStyleEncodeDecodeType(vt))
            {
                sw.WriteLine(" {");
                vt.Accept(new Define("_e_", sw, prefix + "        "));
                vt.Accept(new Decode("_e_", 0, bufName, sw, prefix + "        "));
                sw.WriteLine($"{prefix}        _x_.add(_e_);");
                sw.WriteLine($"{prefix}    }}");
            }
            else
            {
                sw.WriteLine();
                sw.WriteLine(prefix + "        _x_.add(" + DecodeElement(vt, "_t_") + ");");
            }
            sw.WriteLine($"{prefix}}} else");
            sw.WriteLine(prefix + "    " + bufName + ".SkipUnknownFieldOrThrow(_t_, \"Collection\");");
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
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufName + ".ReadByte()) >> ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufName + ".ReadUInt(); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufName, sw, prefix + "        ", "_s_"));
            }
            else
            {
                sw.WriteLine(prefix + "        var _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufName, sw, prefix + "        "));
            }
            else
            {
                sw.WriteLine(prefix + "        var _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
            sw.WriteLine(prefix + "        _x_.put(_k_, _v_);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufName + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufName + ".ReadBean(" + NamePrivate + ", _t_);");
            else
                sw.WriteLine(prefix + NamePrivate + ".decode(" + bufName + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufName + ".ReadBean(" + NamePrivate + ", _t_);");
            else
                sw.WriteLine(prefix + NamePrivate + ".decode(" + bufName + ");");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + bufName + ".ReadDynamic(" + NamePrivate + ", " + typeVarName + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadQuaternion({typeVarName})") + ';');
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadVector2({typeVarName})") + ';');
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadVector2Int({typeVarName})") + ';');
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadVector3({typeVarName})") + ';');
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadVector3Int({typeVarName})") + ';');
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.ReadVector4({typeVarName})") + ';');
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufName}.ReadString(_t_), java.math.MathContext.DECIMAL128)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"new java.math.BigDecimal({bufName}.ReadString(), java.math.MathContext.DECIMAL128)") + ';');
        }
    }
}
