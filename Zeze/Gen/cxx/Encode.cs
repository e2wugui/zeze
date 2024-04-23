using System;
using System.IO;
using Zeze.Gen.Types;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.cxx
{
    public class Encode : Visitor
    {
        readonly Variable var;
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;

        string NameUpper1OrTmp => var != null ? var.NameUpper1 : varname;

        public static void MakeHpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"virtual void Encode(Zeze::ByteBuffer& _o_) const override;");
        }

        public static void MakeCpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"void {bean.Name}::Encode(Zeze::ByteBuffer& _o_) const {{");
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
                sw.WriteLine(prefix + $"    {bean.Base}::encode(_o_);");
            }
            else
                sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "virtual void Encode(Zeze::ByteBuffer& _o_) const override {");
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

        public Encode(Variable var, string varname, int id, string bufname, StreamWriter sw, string prefix)
        {
            this.var = var;
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
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteByte(1);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteBool({NameUpper1OrTmp});");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteInt({NameUpper1OrTmp});");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteInt({NameUpper1OrTmp});");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteInt(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteInt({NameUpper1OrTmp});");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteLong(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteLong({NameUpper1OrTmp});");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteFloat(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteFloat({NameUpper1OrTmp});");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"auto _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_ != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteDouble(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteDouble({NameUpper1OrTmp});");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"const auto& _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (_x_.size() != 0) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteBinary(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteBinary({NameUpper1OrTmp});");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + $"const auto& _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (!_x_.empty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteString({NameUpper1OrTmp});");
        }

        void EncodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Type vt = type.ValueType;
            sw.WriteLine(prefix + "const auto& _x_ = " + var.NameUpper1 + ';');
            sw.WriteLine(prefix + "auto _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    for (auto it = _x_.begin(); it != _x_.end(); ++it) {");
            vt.Accept(new Encode(null, "(*it)", 0, bufname, sw, prefix + "        "));
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw std::exception();");
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
            sw.WriteLine(prefix + "const auto& _x_ = " + var.NameUpper1 + ';');
            sw.WriteLine(prefix + "auto _n_ = _x_.size();");
            sw.WriteLine(prefix + "if (_n_ != 0) {");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteMapType(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    for (auto it = _x_.begin(); it != _x_.end(); ++it) {");
            kt.Accept(new Encode(null, "it->first", 0, bufname, sw, prefix + "        "));
            vt.Accept(new Encode(null, "it->second", 0, bufname, sw, prefix + "        "));
            sw.WriteLine(prefix + "        _n_--;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    if (_n_ != 0)");
            sw.WriteLine(prefix + "        throw std::exception();");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "int _a_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + "int _j_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "int _b_ = " + bufname + ".WriteIndex;");
                sw.WriteLine(prefix + var.NameUpper1 + ".Encode(" + bufname + ");");
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
                sw.WriteLine(prefix + var.NameUpper1 + ".Encode(" + bufname + ");");
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
                sw.WriteLine(prefix + "const auto& _x_ = " + var.NameUpper1 + ';');
                sw.WriteLine(prefix + "if (!_x_.Empty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.Encode(" + bufname + ");");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + varname + ".Encode(" + bufname + ");");
            }
        }

        private void VisitVector(Type type, string typeName)
        {
            if (id > 0)
            {
                sw.WriteLine(prefix + "const auto& _x_ = " + var.NameUpper1 + ';');
                sw.WriteLine(prefix + "if (!_x_.IsZero()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".Write" + typeName + "(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + NameUpper1OrTmp + ".Encode(" + bufname + ");");
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
                sw.WriteLine(prefix + $"const auto& _x_ = {NameUpper1OrTmp};");
                sw.WriteLine(prefix + "if (!_x_.empty()) {");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    " + bufname + ".WriteString(_x_);");
                sw.WriteLine(prefix + "}");
            }
            else
                sw.WriteLine(prefix + bufname + $".WriteString({NameUpper1OrTmp});");
        }
    }
}
