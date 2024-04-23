using System;
using System.IO;
using Zeze.Gen.Types;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.python
{
    public class Encode : Visitor
    {
        readonly Variable var;
        readonly int id;
        readonly string bufName;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly string beanName;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}_PRE_ALLOC_SIZE_ = 16");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def get_pre_alloc_size(self):");
            sw.WriteLine($"{prefix}    return {bean.Name}._PRE_ALLOC_SIZE_");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def set_pre_alloc_size(self, size):");
            sw.WriteLine($"{prefix}    {bean.Name}._PRE_ALLOC_SIZE_ = size");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def encode(self, _o_):");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    _i_ = 0");

            int lastId = 0;
            foreach (var v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    lastId = v.Id;
                }
                v.VariableType.Accept(new Encode(v, v.Id, "_o_", sw, prefix + "    ", bean.Name));
            }

            sw.WriteLine(prefix + "    _o_.write_byte(0)");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}_PRE_ALLOC_SIZE_ = 16");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def get_pre_alloc_size(self):");
            sw.WriteLine($"{prefix}    return {bean.Name}._PRE_ALLOC_SIZE_");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def set_pre_alloc_size(self, size):");
            sw.WriteLine($"{prefix}    {bean.Name}._PRE_ALLOC_SIZE_ = size");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def encode(self, _o_):");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    _i_ = 0");

            int lastId = 0;
            foreach (var v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                if (v.Id > 0)
                {
                    if (v.Id <= lastId)
                        throw new Exception("unordered var.id");
                    lastId = v.Id;
                }
                v.VariableType.Accept(new Encode(v, v.Id, "_o_", sw, prefix + "    ", bean.Name));
            }

            sw.WriteLine($"{prefix}    _o_.write_byte(0)");
        }

        public Encode(Variable var, int id, string bufName, StreamWriter sw, string prefix, string beanName)
        {
            this.var = var;
            this.id = id;
            this.bufName = bufName;
            this.sw = sw;
            this.prefix = prefix;
            this.beanName = beanName;
        }

        public void Visit(TypeBool type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}if self.{var.Name}:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_byte(1)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_bool(self.{var.Name})");
        }

        public void Visit(TypeByte type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_long(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_long(self.{var.Name})");
        }

        public void Visit(TypeShort type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_long(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_long(self.{var.Name})");
        }

        public void Visit(TypeInt type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_long(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_long(self.{var.Name})");
        }

        public void Visit(TypeLong type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_long(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_long(self.{var.Name})");
        }

        public void Visit(TypeFloat type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_float(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_float(self.{var.Name})");
        }

        public void Visit(TypeDouble type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_ != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_double(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_double(self.{var.Name})");
        }

        public void Visit(TypeBinary type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if len(_x_) != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_bytes(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_bytes(self.{var.Name})");
        }

        public void Visit(TypeString type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if len(_x_) != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_string(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_string(self.{var.Name})");
        }

        public void Visit(TypeDecimal type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if len(_x_) != 0:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_string(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_string(self.{var.Name})");
        }

        void EncodeElement(Type type, string prefix, string varName)
        {
            switch (type)
            {
                case TypeBool:
                    sw.WriteLine($"{prefix}{bufName}.write_bool({varName})");
                    break;
                case TypeByte:
                case TypeShort:
                case TypeInt:
                case TypeLong:
                    sw.WriteLine($"{prefix}{bufName}.write_long({varName})");
                    break;
                case TypeFloat:
                    sw.WriteLine($"{prefix}{bufName}.write_float({varName})");
                    break;
                case TypeDouble:
                    sw.WriteLine($"{prefix}{bufName}.write_double({varName})");
                    break;
                case TypeBinary:
                    sw.WriteLine($"{prefix}{bufName}.write_bytes({varName})");
                    break;
                case TypeString:
                    sw.WriteLine($"{prefix}{bufName}.write_string({varName})");
                    break;
                case Bean:
                case BeanKey:
                    sw.WriteLine($"{prefix}{varName}.encode({bufName})");
                    break;
                case TypeDynamic:
                    sw.WriteLine($"{prefix}{bufName}.write_long({beanName}.dynamic_bean2id_{var.Name}({varName}))");
                    sw.WriteLine($"{prefix}{varName}.encode({bufName})");
                    break;
                case TypeVector2:
                    sw.WriteLine($"{prefix}{bufName}.write_vector2({varName})");
                    break;
                case TypeVector2Int:
                    sw.WriteLine($"{prefix}{bufName}.write_vector2int({varName})");
                    break;
                case TypeVector3:
                    sw.WriteLine($"{prefix}{bufName}.write_vector3({varName})");
                    break;
                case TypeVector3Int:
                    sw.WriteLine($"{prefix}{bufName}.write_vector3int({varName})");
                    break;
                case TypeVector4:
                    sw.WriteLine($"{prefix}{bufName}.write_vector4({varName})");
                    break;
                case TypeQuaternion:
                    sw.WriteLine($"{prefix}{bufName}.write_quaternion({varName})");
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
            sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
            sw.WriteLine($"{prefix}_n_ = len(_x_)");
            sw.WriteLine($"{prefix}if _n_ != 0:");
            sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
            sw.WriteLine($"{prefix}    {bufName}.write_list_type(_n_, " + TypeTagName.GetName(vt) + ")");
            sw.WriteLine($"{prefix}    for _v_ in _x_:");
            EncodeElement(vt, prefix + "        ", "_v_");
            sw.WriteLine($"{prefix}        _n_ -= 1");
            sw.WriteLine($"{prefix}    if _n_ != 0:");
            sw.WriteLine($"{prefix}        raise Exception(f\"ConcurrentModification: {{_n_}}\")");
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
            sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
            sw.WriteLine($"{prefix}_n_ = len(_x_)");
            sw.WriteLine($"{prefix}if _n_ != 0:");
            sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
            sw.WriteLine($"{prefix}    {bufName}.write_map_type(_n_, " + TypeTagName.GetName(kt) + ", " + TypeTagName.GetName(vt) + ")");
            sw.WriteLine($"{prefix}    for _k_, _v_ in _x_.items():");
            EncodeElement(kt, prefix + "        ", "_k_");
            EncodeElement(vt, prefix + "        ", "_v_");
            sw.WriteLine($"{prefix}        _n_ -= 1");
            sw.WriteLine($"{prefix}    if _n_ != 0:");
            sw.WriteLine($"{prefix}        raise Exception(f\"ConcurrentModification: {{_n_}}\")");
        }

        public void Visit(Bean type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_a_ = {bufName}.wi");
                sw.WriteLine($"{prefix}_j_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}_b_ = {bufName}.wi");
                sw.WriteLine($"{prefix}self.{var.Name}.encode({bufName})");
                sw.WriteLine($"{prefix}if _b_ + 1 == {bufName}.wi:");
                sw.WriteLine($"{prefix}    {bufName}.wi = _a_");
                sw.WriteLine($"{prefix}else:");
                sw.WriteLine($"{prefix}    _i_ = _j_");
            }
            else
                sw.WriteLine($"{prefix}self.{var.Name}.encode({bufName})");
        }

        public void Visit(BeanKey type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_a_ = {bufName}.wi");
                sw.WriteLine($"{prefix}_j_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}_b_ = {bufName}.wi");
                sw.WriteLine($"{prefix}self.{var.Name}.encode({bufName})");
                sw.WriteLine($"{prefix}if _b_ + 1 == {bufName}.wi:");
                sw.WriteLine($"{prefix}    {bufName}.wi = _a_");
                sw.WriteLine($"{prefix}else:");
                sw.WriteLine($"{prefix}    _i_ = _j_");
            }
            else
                sw.WriteLine($"{prefix}self.{var.Name}.encode({bufName})");
        }

        public void Visit(TypeDynamic type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if _x_.__class__ != EmptyBean:");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_long({beanName}.dynamic_bean2id_{var.Name}(self.{var.Name}))");
                sw.WriteLine($"{prefix}    _x_.encode({bufName})");
            }
            else
            {
                sw.WriteLine($"{prefix}self.{var.Name}.encode({bufName})");
            }
        }

        private void VisitVector(Type type, string typeName)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_x_ = self.{var.Name}");
                sw.WriteLine($"{prefix}if not _x_.is_zero():");
                sw.WriteLine($"{prefix}    _i_ = {bufName}.write_tag(_i_, {id}, {TypeTagName.GetName(type)})");
                sw.WriteLine($"{prefix}    {bufName}.write_{typeName}(_x_)");
            }
            else
                sw.WriteLine($"{prefix}{bufName}.write_{typeName}(_x_)");
        }

        public void Visit(TypeVector2 type)
        {
            VisitVector(type, "vector2");
        }

        public void Visit(TypeVector2Int type)
        {
            VisitVector(type, "vector2int");
        }

        public void Visit(TypeVector3 type)
        {
            VisitVector(type, "vector3");
        }

        public void Visit(TypeVector3Int type)
        {
            VisitVector(type, "vector3int");
        }

        public void Visit(TypeVector4 type)
        {
            VisitVector(type, "vector4");
        }

        public void Visit(TypeQuaternion type)
        {
            VisitVector(type, "quaternion");
        }
    }
}
