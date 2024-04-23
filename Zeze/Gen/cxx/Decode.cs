using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
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

        string NameUpper1OrTmp => var != null ? var.NameUpper1 : tmpvarname;

        public static void MakeHpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "void Decode(Zeze::ByteBuffer& _o_) override;");
        }

        public static void MakeCpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"void {bean.Name}::Decode(Zeze::ByteBuffer& _o_) {{");
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
                sw.WriteLine(prefix + "            base.Decode(_o_);");
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
            sw.WriteLine(prefix + "virtual void Decode(Zeze::ByteBuffer& _o_) override {");
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
                    return $"{var.NameUpper1} = false";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                case TypeFloat:
                case TypeDouble:
                    return $"{var.NameUpper1} = 0";
                case TypeString:
                    return $"{var.NameUpper1} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                    return $"{var.NameUpper1}.Set(0, 0)";
                case TypeVector3:
                case TypeVector3Int:
                    return $"{var.NameUpper1}.Set(0, 0, 0)";
                case TypeVector4:
                case TypeQuaternion:
                    return $"{var.NameUpper1}.Set(0, 0, 0, 0)";
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
            this.typeVarName = "_t_";
        }

        public Decode(string tmpvarname, int id, string bufname, StreamWriter sw, string prefix, string typeVarName = null)
        {
            this.var = null;
            this.tmpvarname = tmpvarname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = typeVarName ?? "_t_";
        }

        string AssignText(string value)
        {
            if (var != null)
                return $"{var.NameUpper1} = {value}";
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

        void DecodeCollection(TypeCollection type, string push_back)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "auto& _x_ = " + var.NameUpper1 + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.Write(prefix + "    for (int _n_ = " + bufname + ".ReadTagSize(_t_ = " + bufname + ".ReadByte()); _n_ > 0; _n_--)");
            sw.WriteLine(" {");
            vt.Accept(new Define("_e_", sw, prefix + "        "));
            vt.Accept(new Decode("_e_", 0, bufname, sw, prefix + "        "));
            sw.WriteLine($"{prefix}        _x_.{push_back}(_e_);");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}}} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Collection\");");
        }

        public void Visit(TypeList type)
        {
            DecodeCollection(type, "push_back");
        }

        public void Visit(TypeSet type)
        {
            DecodeCollection(type, "insert");
        }

        public void Visit(TypeMap type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            Types.Type kt = type.KeyType;
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "auto& _x_ = " + var.NameUpper1 + ';');
            sw.WriteLine(prefix + "_x_.clear();");
            sw.WriteLine(prefix + "if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == " + TypeTagName.GetName(type) + ") {");
            sw.WriteLine(prefix + "    int _s_ = (_t_ = " + bufname + ".ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;");
            sw.WriteLine(prefix + "    for (int _n_ = " + bufname + ".ReadUInt(); _n_ > 0; _n_--) {");
            kt.Accept(new Define("_k_", sw, prefix + "        "));
            kt.Accept(new Decode("_k_", 0, bufname, sw, prefix + "        ", "_s_"));
            vt.Accept(new Define("_v_", sw, prefix + "        "));
            vt.Accept(new Decode("_v_", 0, bufname, sw, prefix + "        "));
            sw.WriteLine(prefix + "        _x_[_k_] = _v_;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "} else");
            sw.WriteLine(prefix + "    " + bufname + ".SkipUnknownFieldOrThrow(_t_, \"Map\");");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + var.NameUpper1 + ", _t_);");
            else
                sw.WriteLine(prefix + NameUpper1OrTmp + ".Decode(" + bufname + ");");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
                sw.WriteLine(prefix + bufname + ".ReadBean(" + var.NameUpper1 + ", _t_);");
            else
                sw.WriteLine(prefix + NameUpper1OrTmp + ".Decode(" + bufname + ");");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + bufname + ".ReadDynamic(" + NameUpper1OrTmp + ", " + typeVarName + ");");
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
            if (id > 0)
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadString(_t_)") + ';');
            else
                sw.WriteLine(prefix + AssignText($"{bufname}.ReadString()") + ';');

        }
    }
}
