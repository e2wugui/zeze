using System;
using System.IO;
using Zeze.Gen.java;
using Zeze.Gen.Types;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.javadata
{
    public class Encode : Visitor
    {
        readonly Variable var;
        readonly string varname;
        readonly int id;
        readonly string bufName;
        readonly StreamWriter sw;
        readonly string prefix;

        string NamePrivate => var != null ? var.NamePrivate : varname;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            if (bean.OnlyData)
            {
                sw.WriteLine(prefix + "private static int _PRE_ALLOC_SIZE_ = 16;");
                sw.WriteLine();
            }
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
                v.VariableType.Accept(new Encode(v, null, v.Id, "_o_", sw, prefix + "        "));
                sw.WriteLine(prefix + "    }");
            }

            if (bean.Base != "")
            {
                sw.WriteLine(prefix + "    _o_.WriteByte(1);");
                sw.WriteLine(prefix + "    super.encode(_o_);");
            }
            else
                sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            // sw.WriteLine(prefix + "private static int _PRE_ALLOC_SIZE_ = 16;");
            // sw.WriteLine();
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
                v.VariableType.Accept(new Encode(v, null, v.Id, "_o_", sw, prefix + "        "));
                sw.WriteLine(prefix + "    }");
            }

            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Encode(Variable var, string varname, int id, string bufName, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.varname = varname;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "boolean _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteByte(1);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteBool(" + NamePrivate + ");");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteInt(" + NamePrivate + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteInt(" + NamePrivate + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteInt(" + NamePrivate + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "long _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteLong(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteLong(" + NamePrivate + ");");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "float _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteFloat(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteFloat(" + NamePrivate + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "double _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteDouble(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteDouble(" + NamePrivate + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_.size() != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteBinary(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteBinary(" + NamePrivate + ");");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "String _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteString(" + NamePrivate + ");");
        }

        void EncodeElement(Type type, string prefix, string varName)
        {
            switch (type)
            {
                case TypeBool:
                    sw.WriteLine(prefix + bufName + ".WriteBool(" + varName + ");");
                    break;
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                    sw.WriteLine(prefix + bufName + ".WriteLong(" + varName + ");");
                    break;
                case TypeFloat:
                    sw.WriteLine(prefix + bufName + ".WriteFloat(" + varName + ");");
                    break;
                case TypeDouble:
                    sw.WriteLine(prefix + bufName + ".WriteDouble(" + varName + ");");
                    break;
                case TypeBinary:
                    sw.WriteLine(prefix + bufName + ".WriteBinary(" + varName + ");");
                    break;
                case TypeString:
                    sw.WriteLine(prefix + bufName + ".WriteString(" + varName + ");");
                    break;
                case Bean:
                case BeanKey:
                case TypeDynamic:
                    sw.WriteLine(prefix + varName + ".encode(" + bufName + ");");
                    break;
                case TypeVector2:
                    sw.WriteLine(prefix + bufName + ".WriteVector2(" + varName + ");");
                    break;
                case TypeVector2Int:
                    sw.WriteLine(prefix + bufName + ".WriteVector2Int(" + varName + ");");
                    break;
                case TypeVector3:
                    sw.WriteLine(prefix + bufName + ".WriteVector3(" + varName + ");");
                    break;
                case TypeVector3Int:
                    sw.WriteLine(prefix + bufName + ".WriteVector3Int(" + varName + ");");
                    break;
                case TypeVector4:
                    sw.WriteLine(prefix + bufName + ".WriteVector4(" + varName + ");");
                    break;
                case TypeQuaternion:
                    sw.WriteLine(prefix + bufName + ".WriteQuaternion(" + varName + ");");
                    break;
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        public void Visit(TypeList type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
            if (!string.IsNullOrEmpty(type.Variable.JavaType) && type.ValueType.Name.StartsWith("vector"))
                sw.WriteLine(prefix + "int _n_ = _x_.vectorSize();");
            else
                sw.WriteLine(prefix + "int _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufName + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            if (!string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + "    _x_.encode(" + bufName + ", _n_);");
            else
            {
                sw.WriteLine(prefix + "    for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {");
                sw.WriteLine(prefix + "        var _v_ = _x_.get(_j_);");
                if (Decode.IsOldStyleEncodeDecodeType(vt))
                    vt.Accept(new Encode(null, "_v_", 0, bufName, sw, prefix + "        "));
                else
                    EncodeElement(vt, prefix + "        ", "_v_");
                sw.WriteLine(prefix + "        _n_--;");
                sw.WriteLine(prefix + "    }");
                sw.WriteLine(prefix + "    if (_n_ != 0)");
                sw.WriteLine(prefix + "        throw new java.util.ConcurrentModificationException(String.valueOf(_n_));");
            }
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeSet type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Type vt = type.ValueType;
            sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
            sw.WriteLine(prefix + "int _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufName + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            if (string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + "    for (var _v_ : _x_) {");
            else
            {
                sw.WriteLine(prefix + "    for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {");
                sw.WriteLine(prefix + "        var _v_ = _j_.value();");
            }
            if (Decode.IsOldStyleEncodeDecodeType(vt))
                vt.Accept(new Encode(null, "_v_", 0, bufName, sw, prefix + "        "));
            else
                EncodeElement(vt, prefix + "        ", "_v_");
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw new java.util.ConcurrentModificationException(String.valueOf(_n_));");
            sw.WriteLine(prefix + "}");
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
            sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufName + ".WriteMapType(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ");");
            if (string.IsNullOrEmpty(type.Variable.JavaType))
            {
                sw.WriteLine(prefix + "    for (var _e_ : _x_.entrySet()) {");
                if (Decode.IsOldStyleEncodeDecodeType(kt))
                    vt.Accept(new Encode(null, "_e_.getKey()", 0, bufName, sw, prefix + "        "));
                else
                    EncodeElement(kt, prefix + "        ", "_e_.getKey()");
                if (Decode.IsOldStyleEncodeDecodeType(vt))
                    vt.Accept(new Encode(null, "_e_.getValue()", 0, bufName, sw, prefix + "        "));
                else
                    EncodeElement(vt, prefix + "        ", "_e_.getValue()");
            }
            else
            {
                sw.WriteLine(prefix + "    for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {");
                if (Decode.IsOldStyleEncodeDecodeType(kt))
                    vt.Accept(new Encode(null, "_j_.key()", 0, bufName, sw, prefix + "        "));
                else
                    EncodeElement(kt, prefix + "        ", "_j_.key()");
                if (Decode.IsOldStyleEncodeDecodeType(vt))
                    vt.Accept(new Encode(null, "_j_.value()", 0, bufName, sw, prefix + "        "));
                else
                    EncodeElement(vt, prefix + "        ", "_j_.value()");
            }
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
                sw.WriteLine(prefix + "int _a_ = " + bufName + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufName + ".WriteIndex;");
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufName + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufName + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _a_ = " + bufName + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufName + ".WriteIndex;");
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
                sw.WriteLine(prefix + "if (_b_ + 1 == " + bufName + ".WriteIndex)");
                sw.WriteLine(prefix + "    " + bufName + ".WriteIndex = _a_;");
                sw.WriteLine(prefix + "else");
                sw.WriteLine(prefix + "    _i_ = _j_;");
            }
            else
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.encode(" + bufName + ");");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
            }
        }

        private void VisitVector(Type type, string typeName)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + NamePrivate + ';');
                sw.WriteLine(prefix + "if (_x_ != null && !_x_.isZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".Write" + typeName + "(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + NamePrivate + ".encode(" + bufName + ");");
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
                sw.WriteLine(prefix + "String _x_ = " + NamePrivate + ".toString();");
                sw.WriteLine(prefix + "if (!_x_.isEmpty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufName + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufName + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufName + ".WriteString(" + NamePrivate + ".toString());");
        }
    }
}
