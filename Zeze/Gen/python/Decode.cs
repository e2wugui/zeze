using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
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
        readonly string beanName;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def decode(self, _o_):");
            sw.WriteLine($"{prefix}    _t_ = _o_.read_byte()");
            sw.WriteLine(bean.VariablesIdOrder.Count > 0
                ? $"{prefix}    _i_ = _o_.read_tag_size(_t_)"
                : $"{prefix}    _o_.read_tag_size(_t_)");

            int lastId = 0;
            foreach (var v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1)
                    {
                        sw.WriteLine($"{prefix}    while (_t_ & 0xff) > 1 and _i_ < " + v.Id + ":");
                        sw.WriteLine($"{prefix}        _o_.skip_unknown_field(_t_)");
                        sw.WriteLine($"{prefix}        _t_ = _o_.read_byte()");
                        sw.WriteLine($"{prefix}        _i_ += _o_.read_tag_size(_t_)");
                    }
                    lastId = v.Id;
                    sw.WriteLine($"{prefix}    if _i_ == {v.Id}:");
                }
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, $"{prefix}        ", bean.Name));
                if (v.Id > 0)
                {
                    sw.WriteLine($"{prefix}        _t_ = _o_.read_byte()");
                    sw.WriteLine($"{prefix}        _i_ += _o_.read_tag_size(_t_)");
                    if (v.Initial.Length > 0)
                    {
                        sw.WriteLine($"{prefix}    else:");
                        sw.WriteLine($"{prefix}        {Initial(v)}");
                    }
                }
            }

            if (bean.Base == "")
                sw.WriteLine($"{prefix}    _o_.skip_all_unknown_fields(_t_)");
            else
            {
                sw.WriteLine($"{prefix}    while _t_ != 0:");
                sw.WriteLine($"{prefix}        if _t_ == 1:");
                sw.WriteLine($"{prefix}            super().decode(_o_)");
                sw.WriteLine($"{prefix}            return");
                sw.WriteLine($"{prefix}        _o_.skip_unknown_field(_t_)");
                sw.WriteLine($"{prefix}        _t_ = _o_.read_byte()");
                sw.WriteLine($"{prefix}        _o_.read_tag_size(_t_)");
            }
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def decode(self, _o_):");
            sw.WriteLine($"{prefix}    _t_ = _o_.read_byte()");
            sw.WriteLine(bean.VariablesIdOrder.Count > 0
                ? $"{prefix}    _i_ = _o_.read_tag_size(_t_)"
                : $"{prefix}    _o_.read_tag_size(_t_)");

            int lastId = 0;
            foreach (var v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    if (v.Id - lastId > 1)
                    {
                        sw.WriteLine($"{prefix}    while (_t_ & 0xff) > 1 and _i_ < " + v.Id + ":");
                        sw.WriteLine($"{prefix}        _o_.skip_unknown_field(_t_)");
                        sw.WriteLine($"{prefix}        _t_ = _o_.read_byte()");
                        sw.WriteLine($"{prefix}        _i_ += _o_.read_tag_size(_t_)");
                    }
                    lastId = v.Id;
                    sw.WriteLine($"{prefix}    if _i_ == {v.Id}:");
                }
                v.VariableType.Accept(new Decode(v, v.Id, "_o_", sw, $"{prefix}        ", bean.Name));
                if (v.Id > 0)
                {
                    sw.WriteLine($"{prefix}        _t_ = _o_.read_byte()");
                    sw.WriteLine($"{prefix}        _i_ += _o_.read_tag_size(_t_)");
                    if (v.Initial.Length > 0)
                    {
                        sw.WriteLine($"{prefix}    else:");
                        sw.WriteLine($"{prefix}        {Initial(v)}");
                    }
                }
            }

            sw.WriteLine($"{prefix}    _o_.skip_all_unknown_fields(_t_)");
        }

        static string Initial(Variable var)
        {
            var type = var.VariableType;
            switch (type)
            {
                case TypeBool:
                    return $"self.{var.Name} = False";
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                case TypeFloat:
                case TypeDouble:
                    return $"self.{var.Name} = 0";
                case TypeString:
                    return $"self.{var.Name} = \"\"";
                case Bean:
                case BeanKey:
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    return $"self.{var.Name}.reset()";
                default:
                    throw new Exception("unsupported initial type: " + var.VariableType);
            }
        }

        public Decode(Variable var, int id, string bufName, StreamWriter sw, string prefix, string beanName)
        {
            this.var = var;
            this.tmpVarName = null;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = "_t_";
            this.beanName = beanName;
        }

        public Decode(string tmpVarName, int id, string bufName, StreamWriter sw, string prefix, string beanName, string typeVarName = null)
        {
            this.var = null;
            this.tmpVarName = tmpVarName;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
            this.typeVarName = typeVarName ?? "_t_";
            this.beanName = beanName;
        }

        string AssignText(string value)
        {
            return var != null ? $"self.{var.Name} = {value}" : $"{tmpVarName} = {value}";
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_bool_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_bool()");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_long_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_long()");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_long_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_long()");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_long_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_long()");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_long_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_long()");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_float_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_float()");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_double_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_double()");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_binary_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_bytes()");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_string_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_string()");
        }

        string DecodeElement(Types.Type type, string typeVar, string varName)
        {
            switch (type)
            {
                case TypeBool:
                    return $"{bufName}.read_bool_tag({typeVar})";
                case TypeByte:
                    return $"{bufName}.read_long_tag({typeVar})";
                case TypeShort:
                    return $"{bufName}.read_long_tag({typeVar})";
                case TypeInt:
                    return $"{bufName}.read_long_tag({typeVar})";
                case TypeLong:
                    return $"{bufName}.read_long_tag({typeVar})";
                case TypeFloat:
                    return $"{bufName}.read_float_tag({typeVar})";
                case TypeDouble:
                    return $"{bufName}.read_double_tag({typeVar})";
                case TypeBinary:
                    return $"{bufName}.read_binary_tag({typeVar})";
                case TypeString:
                    return $"{bufName}.read_string_tag({typeVar})";
                case Bean:
                case BeanKey:
                    return $"{bufName}.read_bean_tag({TypeName.GetName(type)}(), {typeVar})";
                case TypeDynamic:
                    return $"{bufName}.read_dynamic_tag({beanName}.dynamic_id2bean_{varName}, {typeVar})";
                case TypeVector2:
                    return $"{bufName}.read_vector2_tag({typeVar})";
                case TypeVector2Int:
                    return $"{bufName}.read_vector2Int_tag({typeVar})";
                case TypeVector3:
                    return $"{bufName}.read_vector3_tag({typeVar})";
                case TypeVector3Int:
                    return $"{bufName}.read_vector3Int_tag({typeVar})";
                case TypeVector4:
                    return $"{bufName}.read_vector4_tag({typeVar})";
                case TypeQuaternion:
                    return $"{bufName}.read_quaternion_tag({typeVar})";
                default:
                    throw new Exception("invalid collection element type: " + type);
            }
        }

        void DecodeCollection(TypeCollection type)
        {
            if (id <= 0)
                throw new Exception("invalid variable.id");
            var vt = type.ValueType;
            sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
            sw.WriteLine($"{prefix}_x_.clear()");
            sw.WriteLine($"{prefix}if (_t_ & ByteBuffer.TAG_MASK) == {TypeTagName.GetName(type)}:");
            sw.WriteLine($"{prefix}    _t_ = " + bufName + ".read_byte()");
            sw.WriteLine($"{prefix}    _n_ = " + bufName + ".read_tag_size(_t_)");
            sw.WriteLine($"{prefix}    while _n_ > 0:");
            sw.WriteLine($"{prefix}        _x_.append(" + DecodeElement(vt, "_t_", var.Name) + ")");
            sw.WriteLine($"{prefix}        _n_ -= 1");
            sw.WriteLine($"{prefix}else:");
            sw.WriteLine($"{prefix}    " + bufName + ".skip_unknown_field_or_raise(_t_, \"Collection\")");
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
            var kt = type.KeyType;
            var vt = type.ValueType;
            sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
            sw.WriteLine($"{prefix}_x_.clear()");
            sw.WriteLine($"{prefix}if (_t_ & ByteBuffer.TAG_MASK) == {TypeTagName.GetName(type)}:");
            sw.WriteLine($"{prefix}    _t_ = " + bufName + ".read_byte()");
            sw.WriteLine($"{prefix}    _s_ = _t_ >> ByteBuffer.TAG_SHIFT");
            sw.WriteLine($"{prefix}    _n_ = {bufName}.read_uint()");
            sw.WriteLine($"{prefix}    while _n_ > 0:");
            sw.WriteLine($"{prefix}        _k_ = " + DecodeElement(kt, "_s_", var.Name));
            sw.WriteLine($"{prefix}        _v_ = " + DecodeElement(vt, "_t_", var.Name));
            sw.WriteLine($"{prefix}        _x_[_k_] = _v_");
            sw.WriteLine($"{prefix}        _n_ -= 1");
            sw.WriteLine($"{prefix}else:");
            sw.WriteLine($"{prefix}    " + bufName + ".skip_unknown_field_or_raise(_t_, \"Map\")");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}{bufName}.read_bean_tag(self.{var.Name}, _t_)"
                : $"{prefix}self.{var.Name}.decode({bufName})");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}{bufName}.read_bean_tag(self.{var.Name}, _t_)"
                : $"{prefix}self.{var.Name}.decode({bufName})");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}{bufName}.read_dynamic_tag({beanName}.dynamic_id2bean_{var.Name}, {typeVarName})");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_quaternion({typeVarName})"));
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_vector2({typeVarName})"));
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_vector2int({typeVarName})"));
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_vector3({typeVarName})"));
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_vector3int({typeVarName})"));
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + AssignText($"{bufName}.read_vector4({typeVarName})"));
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(id > 0
                ? $"{prefix}self.{var.Name} = {bufName}.read_string_tag(_t_)"
                : $"{prefix}self.{var.Name} = {bufName}.read_string()");
        }
    }
}
