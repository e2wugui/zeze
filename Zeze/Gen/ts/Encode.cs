using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Encode : Visitor
    {
        readonly string varname;
        readonly string bufname;
        readonly int id;
        readonly StreamWriter sw;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Encode(_o_: Zeze.ByteBuffer) {");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    let _i_ = 0;");
                foreach (Variable v in bean.VariablesIdOrder)
                {
                    if (v.Transient)
                        continue;

                    sw.WriteLine(prefix + "    {");
                    v.VariableType.Accept(new Encode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
                    sw.WriteLine(prefix + "    }");
                }
            }
            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public Encode(_o_: Zeze.ByteBuffer) {");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    let _i_ = 0;");
                foreach (Variable v in bean.VariablesIdOrder)
                {
                    if (v.Transient)
                        continue;

                    sw.WriteLine(prefix + "    {");
                    v.VariableType.Accept(new Encode("this." + v.Name, v.Id, "_o_", sw, prefix + "        "));
                    sw.WriteLine(prefix + "    }");
                }
            }
            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Encode(string varname, int id, string bufname, StreamWriter sw, string prefix)
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
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteByte(1);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteBool(" + varname + ");");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0n) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteLong(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteLong(" + varname + ");");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteFloat(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteFloat(" + varname + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteDouble(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteDouble(" + varname + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_.length !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteBytes(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteBytes(" + varname + ");");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_.length !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ");");
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_.length !== 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ");");
        }

        void EncodeElement(Types.Type type, string prefix, string varName)
        {
            switch (type)
            {
                case TypeBool:
                    sw.WriteLine(prefix + bufname + ".WriteBool(" + varName + ");");
                    break;
                case TypeByte:
                case TypeShort:
                case TypeInt:
                    sw.WriteLine(prefix + bufname + ".WriteInt(" + varName + ");");
                    break;
                case TypeLong:
                    sw.WriteLine(prefix + bufname + ".WriteLong(" + varName + ");");
                    break;
                case TypeFloat:
                    sw.WriteLine(prefix + bufname + ".WriteFloat(" + varName + ");");
                    break;
                case TypeDouble:
                    sw.WriteLine(prefix + bufname + ".WriteDouble(" + varName + ");");
                    break;
                case TypeBinary:
                    sw.WriteLine(prefix + bufname + ".WriteBytes(" + varName + ");");
                    break;
                case TypeString:
                    sw.WriteLine(prefix + bufname + ".WriteString(" + varName + ");");
                    break;
                case Bean:
                case BeanKey:
                case TypeDynamic:
                    sw.WriteLine(prefix + varName + ".Encode(" + bufname + ");");
                    break;
                case TypeVector2:
                    sw.WriteLine(prefix + bufname + ".WriteVector2(" + varName + ");");
                    break;
                case TypeVector2Int:
                    sw.WriteLine(prefix + bufname + ".WriteVector2(" + varName + ");");
                    break;
                case TypeVector3:
                    sw.WriteLine(prefix + bufname + ".WriteVector3(" + varName + ");");
                    break;
                case TypeVector3Int:
                    sw.WriteLine(prefix + bufname + ".WriteVector3(" + varName + ");");
                    break;
                case TypeVector4:
                    sw.WriteLine(prefix + bufname + ".WriteVector4(" + varName + ");");
                    break;
                case TypeQuaternion:
                    sw.WriteLine(prefix + bufname + ".WriteVector4(" + varName + ");");
                    break;
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public void Visit(TypeList type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_ = " + varname + ';');
            sw.WriteLine(prefix + "const _n_ = _x_.length;");
            sw.WriteLine(prefix + "if (_n_ !== 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _x_.forEach(_v_ => {");
            // if (Decode.IsOldStyleEncodeDecodeType(vt))
            // {
            //     vt.Accept(new Encode("_v_", 0, bufname, sw, prefix + "        "));
            // }
            // else
            {
                EncodeElement(vt, prefix + "        ", "_v_");
            }
            sw.WriteLine(prefix + "    });");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeSet type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_ = " + varname + ';');
            sw.WriteLine(prefix + "const _n_ = _x_.size;");
            sw.WriteLine(prefix + "if (_n_ !== 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _x_.forEach(_v_ => {");
            if (Decode.IsOldStyleEncodeDecodeType(vt))
            {
                vt.Accept(new Encode("_v_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                EncodeElement(vt, prefix + "        ", "_v_");
            }
            sw.WriteLine(prefix + "    });");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "const _x_ = " + varname + ';');
            sw.WriteLine(prefix + "const _n_ = _x_.size;");
            sw.WriteLine(prefix + "if (_n_ !== 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteMapType(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    _x_.forEach((_v_, _k_) => {");
            if (Decode.IsOldStyleEncodeDecodeType(kt))
            {
                vt.Accept(new Encode("_k_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                EncodeElement(kt, prefix + "        ", "_k_");
            }
            if (Decode.IsOldStyleEncodeDecodeType(kt))
            {
                vt.Accept(new Encode("_v_", 0, bufname, sw, prefix + "        "));
            }
            else
            {
                EncodeElement(vt, prefix + "        ", "_v_");
            }
            sw.WriteLine(prefix + "    });");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "const _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "const _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 === " + bufname + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufname + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "const _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "const _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 === " + bufname + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufname + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.Encode(" + bufname + ");");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
            }
        }

        public void Visit(TypeVector2 type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector2(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector2(" + varname + ");");
        }

        public void Visit(TypeVector2Int type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector2(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector2(" + varname + ");");
        }

        public void Visit(TypeVector3 type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector3(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector3(" + varname + ");");
        }

        public void Visit(TypeVector3Int type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector3(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector3(" + varname + ");");
        }

        public void Visit(TypeVector4 type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector4(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector4(" + varname + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteVector4(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteVector4(" + varname + ");");
        }
    }
}
