using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
{
    public class Encode : Visitor
    {
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void Encode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    int _i_ = 0;");
                foreach (Variable v in bean.VariablesIdOrder)
                {
                    if (v.Transient)
                        continue;

                    sw.WriteLine(prefix + "    {");
                    v.VariableType.Accept(new Encode(v.NameUpper1, v.Id, "_o_", sw, prefix + "        "));
                    sw.WriteLine(prefix + "    }");
                }
            }
            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Encode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    int _i_ = 0;");
                foreach (Variable v in bean.VariablesIdOrder)
                {
                    sw.WriteLine(prefix + "    {");
                    v.VariableType.Accept(new Encode(v.NamePrivate, v.Id, "_o_", sw, prefix + "        "));
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
                sw.WriteLine(prefix + "bool _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "int _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "int _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "int _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "long _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "float _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "double _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
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
                sw.WriteLine(prefix + "var _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_.Count != 0)");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteBinary(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteBinary(" + varname + ");");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "string _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_.Length != 0)");
                sw.WriteLine(prefix + "{");
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
                    sw.WriteLine(prefix + bufname + ".WriteBinary(" + varName + ");");
                    break;
                case TypeString:
                    sw.WriteLine(prefix + bufname + ".WriteString(" + varName + ");");
                    break;
                case Bean:
                case BeanKey:
                case TypeDynamic:
                    sw.WriteLine(prefix + varName + ".Encode(" + bufname + ");");
                    break;
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        void EncodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            sw.WriteLine(prefix + "int _n_ = _x_.Count;");
            sw.WriteLine(prefix + "if (_n_ != 0)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    foreach (var _v_ in _x_)");
            sw.WriteLine(prefix + "    {");
            EncodeElement(vt, prefix + "        ", "_v_");
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw new System.Exception(_n_.ToString());");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeList type)
        {
            EncodeCollection(type);
        }

        public void Visit(TypeSet type)
        {
            EncodeCollection(type);
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            sw.WriteLine(prefix + "int _n_ = _x_.Count;");
            sw.WriteLine(prefix + "if (_n_ != 0)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteMapType(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    foreach (var _e_ in _x_)");
            sw.WriteLine(prefix + "    {");
            EncodeElement(kt, prefix + "        ", "_e_.Key");
            EncodeElement(vt, prefix + "        ", "_e_.Value");
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw new System.Exception(_n_.ToString());");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufname + ".WriteIndex)");
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
                sw.WriteLine(prefix + "int _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufname + ".WriteIndex)");
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
                sw.WriteLine(prefix + "var _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.IsEmpty())");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.Encode(" + bufname + ");");
                sw.WriteLine(prefix + "}");
            }
            else
                throw new Exception("invalid variable.id");
        }

        public void Visit(TypeQuaternion type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != 0)");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_.ToString());");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ".ToString());");
        }
    }
}
