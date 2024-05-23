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
            sw.WriteLine(prefix + "public void followerApply(Zeze.Transaction.Log log) {");
            if (needApplyVars)
            {
                sw.WriteLine(prefix + "    var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();");
                sw.WriteLine(prefix + "    if (vars == null)");
                sw.WriteLine(prefix + "        return;");
                sw.WriteLine(prefix + "    for (var it = vars.iterator(); it.moveToNext(); ) {");
                sw.WriteLine(prefix + "        var vlog = it.value();");
                sw.WriteLine(prefix + "        switch (vlog.getVariableId()) {");
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
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.booleanValue(); break;");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.byteValue(); break;");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.shortValue(); break;");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.intValue(); break;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.longValue(); break;");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.floatValue(); break;");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.doubleValue(); break;");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.binaryValue(); break;");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.stringValue(); break;");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(vlog); break;");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(vlog); break;");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(vlog); break;");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(vlog); break;");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.followerApply(vlog); break;");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.quaternionValue(); break;");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.vector2Value(); break;");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.vector2IntValue(); break;");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.vector3Value(); break;");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.vector3IntValue(); break;");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.vector4Value(); break;");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.decimalValue(); break;");
        }

        public FollowerApply(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
