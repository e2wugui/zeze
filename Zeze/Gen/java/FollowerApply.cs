using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class FollowerApply : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            bool needApplyVars = false;
            foreach (var v in bean.Variables)
            {
                if (!v.Transient)
                    needApplyVars = true;
            }
            sw.WriteLine(prefix + "@SuppressWarnings(\"unchecked\")");
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void followerApply(Zeze.Transaction.Log _l_) {");
            if (needApplyVars)
            {
                sw.WriteLine(prefix + "    var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();");
                sw.WriteLine(prefix + "    if (_vs_ == null)");
                sw.WriteLine(prefix + "        return;");
                sw.WriteLine(prefix + "    for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {");
                sw.WriteLine(prefix + "        var _v_ = _i_.value();");
                sw.WriteLine(prefix + "        switch (_v_.getVariableId()) {");
                foreach (var v in bean.Variables)
                {
                    if (v.Transient)
                        continue;
                    if (bean.Version.Equals(v.Name))
                        continue; // 版本变量不需要生成FollowerApply实现。
                    v.VariableType.Accept(new FollowerApply(v, sw, prefix + "        "));
                }

                sw.WriteLine(prefix + "        }");
                sw.WriteLine(prefix + "    }");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.booleanValue(); break;");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.byteValue(); break;");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.shortValue(); break;");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.intValue(); break;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.longValue(); break;");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.floatValue(); break;");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.doubleValue(); break;");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.binaryValue(); break;");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.stringValue(); break;");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(_v_); break;");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(_v_); break;");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(_v_); break;");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(_v_); break;");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})_v_).value; break;");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(_v_); break;");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.quaternionValue(); break;");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.vector2Value(); break;");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.vector2IntValue(); break;");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.vector3Value(); break;");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.vector3IntValue(); break;");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.vector4Value(); break;");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = _v_.decimalValue(); break;");
        }

        public FollowerApply(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
