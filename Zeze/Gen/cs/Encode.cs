using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Encode : Visitor
    {
        readonly string varname;
        readonly int id;
        readonly string bufname;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly string varUpperName1;
        readonly int parentId;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool varNameUpper = true)
        {
            sw.WriteLine(prefix + "public override void Encode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = 0;");
            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Encode(varNameUpper ? v.NameUpper1 : v.Name, v.Id, "_o_", sw, prefix + "        ", v.NameUpper1));
                sw.WriteLine(prefix + "    }");
            }
            if (bean.Base != "")
            {
                sw.WriteLine(prefix + "    _o_.WriteByte(1);");
                sw.WriteLine(prefix + "    base.Encode(_o_);");
            }
            else
                sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Encode(ByteBuffer _o_)");
            sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
                sw.WriteLine(prefix + "    int _i_ = 0;");
            foreach (Variable v in bean.VariablesIdOrder)
            {
                if (v.Transient)
                    continue;

                sw.WriteLine(prefix + "    {");
                v.VariableType.Accept(new Encode(v.NamePrivate, v.Id, "_o_", sw, prefix + "        ", v.NameUpper1));
                sw.WriteLine(prefix + "    }");
            }
            sw.WriteLine(prefix + "    _o_.WriteByte(0);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Encode(string varname, int id, string bufname, StreamWriter sw, string prefix, string varUpperName1, int parentId = 0)
        {
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
            this.varUpperName1 = varUpperName1;
            this.parentId = parentId;
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
                case TypeVector2:
                case TypeVector2Int:
                case TypeVector3:
                case TypeVector3Int:
                case TypeVector4:
                case TypeQuaternion:
                    if (Project.MakingInstance.IsUnity)
                        sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varName}, {bufname});");
                    else
                    {
                        sw.WriteLine($"{prefix}if ({varName} != null)");
                        sw.WriteLine($"{prefix}    {varName}.Encode({bufname});");
                        sw.WriteLine($"{prefix}else");
                        sw.WriteLine($"{prefix}    {TypeName.GetName(type)}.EncodeNull({bufname});");
                    }
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
            if (type is TypeArray && vt is TypeByte)
            {
                sw.WriteLine(prefix + "var _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (_x_ != null && _x_.Length != 0)");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", ByteBuffer.BYTES);");
                sw.WriteLine(prefix + "    " + bufname + ".WriteBytes(_x_);");
                sw.WriteLine(prefix + "}");
                return;
            }
            bool isArray = type is TypeArray || type is TypeList list && list.FixSize >= 0;
            sw.WriteLine(prefix + "var _x_ = " + varname + ';');
            if (isArray)
                sw.WriteLine(prefix + "int _n_ = _x_?.Length ?? 0;");
            else
                sw.WriteLine(prefix + "int _n_ = _x_.Count;");
            sw.WriteLine(prefix + "if (_n_ != 0)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
            sw.WriteLine(prefix + "    " + bufname + ".WriteListType(_n_, " + TypeTagName.GetName(vt) + ");");
            sw.WriteLine(prefix + "    foreach (var _v_ in _x_)");
            sw.WriteLine(prefix + "    {");
            if (Decode.IsOldStyleEncodeDecodeType(vt))
                vt.Accept(new Encode("_v_", 0, bufname, sw, prefix + "        ", varUpperName1, id));
            else
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
            if (Decode.IsOldStyleEncodeDecodeType(kt))
                vt.Accept(new Encode("_e_.Key", 0, bufname, sw, prefix + "        ", varUpperName1, id));
            else
                EncodeElement(kt, prefix + "        ", "_e_.Key");
            if (Decode.IsOldStyleEncodeDecodeType(vt))
                vt.Accept(new Encode("_e_.Value", 0, bufname, sw, prefix + "        ", varUpperName1, id));
            else
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
            if (Project.MakingInstance.Platform.StartsWith("conf+cs"))
            {
                if (id > 0)
                {
                    sw.WriteLine($"{prefix}var _x_ = {varname};");
                    sw.WriteLine($"{prefix}if (_x_ != null)");
                    sw.WriteLine($"{prefix}{{");
                    sw.WriteLine($"{prefix}    _i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                    sw.WriteLine($"{prefix}    {bufname}.WriteLong(GetSpecialTypeIdFromBean_{id}(_x_));");
                    sw.WriteLine($"{prefix}    _x_.Encode({bufname});");
                    sw.WriteLine($"{prefix}}}");
                }
                else
                {
                    sw.WriteLine($"{prefix}{bufname}.WriteLong(GetSpecialTypeIdFromBean_{parentId}({varname}));");
                    sw.WriteLine($"{prefix}{varname}.Encode({bufname});");
                }
            }
            else if (id > 0)
            {
                sw.WriteLine(prefix + "var _x_ = " + varname + ';');
                sw.WriteLine(prefix + "if (!_x_.IsEmpty())");
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    _i_ = " + bufname + ".WriteTag(_i_, " + id + ", " + TypeTagName.GetName(type) + ");");
                sw.WriteLine(prefix + "    _x_.Encode(" + bufname + ");");
                sw.WriteLine(prefix + "}");
            }
            else
            {
                sw.WriteLine(prefix + "_x_.Encode(" + bufname + ");");
            }
        }

        public void Visit(TypeQuaternion type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Quaternion.EncodeNull({bufname});");
            }
        }

        public void Visit(TypeVector2 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Vector2.EncodeNull({bufname});");
            }
        }

        public void Visit(TypeVector2Int type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Vector2Int.EncodeNull({bufname});");
            }
        }

        public void Visit(TypeVector3 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Vector3.EncodeNull({bufname});");
            }
        }

        public void Visit(TypeVector3Int type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Vector3Int.EncodeNull({bufname});");
            }
        }

        public void Visit(TypeVector4 type)
        {
            if (id > 0)
            {
                sw.WriteLine($"{prefix}_i_ = {bufname}.WriteTag(_i_, {id}, {TypeTagName.GetName(type)});");
                sw.WriteLine(Project.MakingInstance.IsUnity
                    ? $"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});"
                    : $"{prefix}{varname}.Encode({bufname});");
            }
            else if (Project.MakingInstance.IsUnity)
                sw.WriteLine($"{prefix}Zeze.Util.UnityVectorHelper.Encode({varname}, {bufname});");
            else
            {
                sw.WriteLine($"{prefix}if ({varname} != null)");
                sw.WriteLine($"{prefix}    {varname}.Encode({bufname});");
                sw.WriteLine($"{prefix}else");
                sw.WriteLine($"{prefix}    Zeze.Serialize.Vector4.EncodeNull({bufname});");
            }
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
