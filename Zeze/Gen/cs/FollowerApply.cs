using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class FollowerApply : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine(prefix + $"public override void FollowerApply(Zeze.Transaction.Log log)");
            sw.WriteLine(prefix + "{");
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine(prefix + "    var blog = (Zeze.Transaction.Collections.LogBean)log;");
                sw.WriteLine(prefix + "    foreach (var vlog in blog.Variables.Values)");
                sw.WriteLine(prefix + "    {");
                sw.WriteLine(prefix + "        switch (vlog.VariableId)");
                sw.WriteLine(prefix + "        {");
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
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.BoolValue(); break;");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.ByteValue(); break;");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.ShortValue(); break;");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.IntValue(); break;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.LongValue(); break;");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.FloatValue(); break;");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.DoubleValue(); break;");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.BinaryValue(); break;");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.StringValue(); break;");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.QuaternionValue(); break;");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.Vector2Value(); break;");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.Vector2IntValue(); break;");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.Vector3Value(); break;");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.Vector3IntValue(); break;");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.Vector4Value(); break;");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = vlog.DecimalValue(); break;");
        }

        public FollowerApply(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
