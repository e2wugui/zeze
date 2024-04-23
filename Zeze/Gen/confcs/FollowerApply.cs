using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
{
    public class FollowerApply : Types.Visitor
    {
        readonly StreamWriter sw;
        readonly Types.Variable var;
        readonly string prefix;

        public static void Make(Types.Bean bean, StreamWriter sw, string prefix)
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
                    v.VariableType.Accept(new FollowerApply(v, sw, prefix + "        "));
                }
                sw.WriteLine(prefix + "        }");
                sw.WriteLine(prefix + "    }");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(Types.TypeBool type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeList type)
        {
            var v = type.ValueType.IsNormalBean ? "2" : "1";
            sw.WriteLine(prefix + $"    case {var.Id}: Zeze.Transaction.Collections.CollApply.ApplyList{v}({var.NameUpper1}, vlog); break;");
        }

        public void Visit(Types.TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: Zeze.Transaction.Collections.CollApply.ApplySet1({var.NameUpper1}, vlog); break;");
        }

        public void Visit(Types.TypeMap type)
        {
            var v = type.ValueType.IsNormalBean ? "2" : "1";
            sw.WriteLine(prefix + $"    case {var.Id}: Zeze.Transaction.Collections.CollApply.ApplyMap{v}({var.NameUpper1}, vlog); break;");
        }

        public void Visit(Types.Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: Zeze.Transaction.Collections.CollApply.ApplyOne<{type.FullName}>(ref {var.NameUpper1}, vlog); break;");
        }

        public void Visit(Types.BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NameUpper1} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public FollowerApply(Types.Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
