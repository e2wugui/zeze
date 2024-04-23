using System;
using System.IO;
using Zeze.Gen.Types;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.java
{
    public class Encode : Visitor
    {
        readonly Variable var;
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly bool isData;

        string Getter => var != null ? isData ? var.NamePrivate : var.Getter : varname;
        string NamePrivate => var != null ? var.NamePrivate : varname;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool withUnknown)
        {
            sw.WriteLine(prefix + "private static int _PRE_ALLOC_SIZE_ = 16;");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int preAllocSize() {");
            sw.WriteLine(prefix + "    return _PRE_ALLOC_SIZE_;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void preAllocSize(int size) {");
            sw.WriteLine(prefix + "    _PRE_ALLOC_SIZE_ = size;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            if (withUnknown)
            {
                sw.WriteLine(prefix + "private byte[] _unknown_;");
                sw.WriteLine();
                sw.WriteLine(prefix + "public byte[] unknown() {");
                sw.WriteLine(prefix + "    return _unknown_;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + "public void clearUnknown() {");
                sw.WriteLine(prefix + "    _unknown_ = null;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encode(ByteBuffer _o_) {");
            if (withUnknown)
            {
                sw.WriteLine(prefix + "    ByteBuffer _u_ = null;");
                sw.WriteLine(prefix + "    var _ua_ = _unknown_;");
                sw.WriteLine(prefix + "    var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;");
                sw.WriteLine(prefix + "    int _i_ = 0;");
            }
            else if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = 0;");

            int lastId = 0;
            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1 && withUnknown)
                    {
                        sw.WriteLine(prefix + "    while (_ui_ < " + v.Id + ") {");
                        sw.WriteLine(prefix + "        _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);");
                        sw.WriteLine(prefix + "        _ui_ = _u_.readUnknownIndex();");
                        sw.WriteLine(prefix + "    }");
                    }
                    lastId = v.Id;
                }
                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Encode(v, null, v.Id, "_o_", sw, prefix + "        ", false));
                sw.WriteLine(prefix + "    }");
            }

            if (withUnknown)
            {
                sw.WriteLine(prefix + "    _o_.writeAllUnknownFields(_i_, _ui_, _u_);");
                sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            }
            else
            {
                sw.WriteLine(prefix + "    _o_.WriteByte(1);");
                sw.WriteLine(prefix + "    super.encode(_o_);");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "private static int _PRE_ALLOC_SIZE_ = 16;");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int preAllocSize() {");
            sw.WriteLine(prefix + "    return _PRE_ALLOC_SIZE_;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void preAllocSize(int size) {");
            sw.WriteLine(prefix + "    _PRE_ALLOC_SIZE_ = size;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encode(ByteBuffer _o_) {");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = 0;");

            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Encode(v, null, v.Id, "_o_", sw, prefix + "        ", true));
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Encode(Variable var, string varname, int id, string bufname, StreamWriter sw, string prefix, bool isData)
        {
            this.var = var;
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.isData = isData;
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "boolean _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteByte(1);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteBool(" + Getter + ");");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + Getter + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + Getter + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteInt(" + Getter + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "long _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteLong(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteLong(" + Getter + ");");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "float _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteFloat(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteFloat(" + Getter + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "double _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteDouble(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteDouble(" + Getter + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_.size() != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteBinary(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteBinary(" + Getter + ");");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "String _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteString(" + Getter + ");");
        }

        void EncodeElement(Type type, string prefix, string varName)
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
                    sw.WriteLine(prefix + varName + ".encode(" + bufname + ");");
                    break;
                case TypeVector2:
                    sw.WriteLine(prefix + bufname + ".WriteVector2(" + varName + ");");
                    break;
                case TypeVector2Int:
                    sw.WriteLine(prefix + bufname + ".WriteVector2Int(" + varName + ");");
                    break;
                case TypeVector3:
                    sw.WriteLine(prefix + bufname + ".WriteVector3(" + varName + ");");
                    break;
                case TypeVector3Int:
                    sw.WriteLine(prefix + bufname + ".WriteVector3Int(" + varName + ");");
                    break;
                case TypeVector4:
                    sw.WriteLine(prefix + bufname + ".WriteVector4(" + varName + ");");
                    break;
                case TypeQuaternion:
                    sw.WriteLine(prefix + bufname + ".WriteQuaternion(" + varName + ");");
                    break;
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        void EncodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
            sw.WriteLine(prefix + "int _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    for (var _v_ : _x_) {");
            if (Decode.IsOldStyleEncodeDecodeType(vt))
                vt.Accept(new Encode(null, "_v_", 0, bufname, sw, prefix + "        ", isData));
            else
                EncodeElement(vt, prefix + "        ", "_v_");
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw new java.util.ConcurrentModificationException(String.valueOf(_n_));");
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
            Type kt = type.KeyType;
            Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
            sw.WriteLine(prefix + "int _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteMapType(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    for (var _e_ : _x_.entrySet()) {");
            if (Decode.IsOldStyleEncodeDecodeType(kt))
                vt.Accept(new Encode(null, "_e_.getKey()", 0, bufname, sw, prefix + "        ", isData));
            else
                EncodeElement(kt, prefix + "        ", "_e_.getKey()");
            if (Decode.IsOldStyleEncodeDecodeType(vt))
                vt.Accept(new Encode(null, "_e_.getValue()", 0, bufname, sw, prefix + "        ", isData));
            else
                EncodeElement(vt, prefix + "        ", "_e_.getValue()");
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw new java.util.ConcurrentModificationException(String.valueOf(_n_));");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufname + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufname + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + Getter + ".encode(" + bufname + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufname + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufname + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + Getter + ".encode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.encode(" + bufname + ");");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufname + ");");
            }
        }

        private void VisitVector(Type type, string typeName)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + Getter + ';');
                sw.WriteLine(prefix + "if (_x_ != null && !_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".Write" + typeName + "(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + Getter + ".encode(" + bufname + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            VisitVector(type, "Quaternion");
        }

        public void Visit(TypeVector2 type)
        {
            VisitVector(type, "Vector2");
        }

        public void Visit(TypeVector2Int type)
        {
            VisitVector(type, "Vector2Int");
        }

        public void Visit(TypeVector3 type)
        {
            VisitVector(type, "Vector3");
        }

        public void Visit(TypeVector3Int type)
        {
            VisitVector(type, "Vector3Int");
        }

        public void Visit(TypeVector4 type)
        {
            VisitVector(type, "Vector4");
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "String _x_ = " + Getter + ".toString();");
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + ".WriteString(" + Getter + ".toString());");
        }
    }
}
