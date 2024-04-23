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
            sw.WriteLine(prefix + "    let _t_ = _o_.ReadByte();");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    let _i_ = _o_.ReadTagSize(_t_);");
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
                         sw.WriteLine(prefix + "    while (_t_ !== 0 && _i_ < " + v.Id + ") {");
                         sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
                         sw.WriteLine(prefix + "        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());");
                         sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                    sw.WriteLine(prefix + "    if (_i_ === " + v.Id + ") {");
                }
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
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

            sw.WriteLine(prefix + "    while (_t_ !== 0) {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Decode(_o_: Zeze.ByteBuffer) {");
            sw.WriteLine(prefix + "    let _t_ = _o_.ReadByte();");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    let _i_ = _o_.ReadTagSize(_t_);");
            else
                sw.WriteLine(prefix + "    _o_.ReadTagSize(_t_);");

            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                    sw.WriteLine(prefix + "    if (_i_ === " + v.Id + ") {");
                else
                    sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Decode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
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

            sw.WriteLine(prefix + "    while (_t_ !== 0) {");
            sw.WriteLine(prefix + "        _o_.SkipUnknownField(_t_);");
            sw.WriteLine(prefix + "        _o_.ReadTagSize(_t_ = _o_.ReadByte());");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
        }

        static string Initial(Variable var)
        {
            var type = var.VariableType;
            switch (type)
            {
                case TypeBool:
                    return $"this.{var.Name} = false";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeFloat:
                case TypeDouble:
                    return $"this.{var.Name} = 0";
                case TypeLong:
                    return $"this.{var.Name} = 0n";
                case TypeString:
                    return $"this.{var.Name} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    return $"this.{var.Name}.reset()";
                default:
                    throw new Exception("unsupported initial type: " + var.VariableType);
            }
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

        public void Visit(TypeDecimal type)
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
                    return bufname + ".ReadBean(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeDynamic:
                    return bufname + ".ReadDynamic(new " + TypeName.GetName(type) + "(), " + typeVar + ')';
                case TypeVector2:
                    return bufname + ".ReadVector2T(" + typeVar + ')';
                case TypeVector2Int:
                    return bufname + ".ReadVector2T(" + typeVar + ')';
                case TypeVector3:
                    return bufname + ".ReadVector3T(" + typeVar + ')';
                case TypeVector3Int:
                    return bufname + ".ReadVector3T(" + typeVar + ')';
                case TypeVector4:
                    return bufname + ".ReadVector4T(" + typeVar + ')';
                case TypeQuaternion:
                    return bufname + ".ReadVector4T(" + typeVar + ')';
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public static bool IsOldStyleEncodeDecodeType(Types.Type type)
        {
            if (type is TypeDynamic)
                return true;
            /*
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
            */
            return false;
        }

        public void Visit(TypeList type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_: " + TypeName.GetName(type) + " = [];");
            sw.WriteLine(prefix + varname + " = _x_;");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) === " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    for (let _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_e_", sw, prefix + "        "));
                vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        "));
                sw.WriteLine(prefix + "        _x_.push(_e_);");
            }
            else
            {
                sw.WriteLine(prefix + "        _x_.push(" + DecodeElement(vt, "_t_") + ");");
            }
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(TypeSet type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_ = " + varname + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) === " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    for (let _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_e_", sw, prefix + "        "));
                vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        "));
                sw.WriteLine(prefix + "        _x_.add(_e_);");
            }
            else
            {
                sw.WriteLine(prefix + "        _x_.add(" + DecodeElement(vt, "_t_") + ");");
            }
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownField(_t_);");
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_ = " + varname + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze.ByteBuffer.TAG_MASK) === " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    const _s_ = (_t_ = " + bufname + ".ReadByte()) >> Zeze.ByteBuffer.TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (let _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--) {");
            if (IsOldStyleEncodeDecodeType(kt))
            {
                kt.Accept(new Define("_k_", sw, prefix + "        "));
                kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                sw.WriteLine(prefix + "        const _k_ = " + DecodeElement(kt, "_s_") + ';');
            }
            if (IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Define("_v_", sw, prefix + "        "));
                vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                sw.WriteLine(prefix + "        const _v_ = " + DecodeElement(vt, "_t_") + ';');
            }
            sw.WriteLine(prefix + "        _x_.set(_k_, _v_);");
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
                sw.WriteLine(prefix + varname + ".Decode(" + bufname + ");");
        }

        public void Visit(TypeVector2 type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector2T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector2(" + varname + ");");
        }

        public void Visit(TypeVector2Int type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector2T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector2(" + varname + ");");
        }

        public void Visit(TypeVector3 type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector3T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector3(" + varname + ");");
        }

        public void Visit(TypeVector3Int type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector3T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector3(" + varname + ");");
        }

        public void Visit(TypeVector4 type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector4T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector4(" + varname + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            if (id > 0)
                sw.WriteLine(prefix + varname + " = " + bufname + ".ReadVector4T(_t_);");
            else
                sw.WriteLine(prefix + bufname + ".ReadVector4(" + varname + ");");
        }
    }
}
