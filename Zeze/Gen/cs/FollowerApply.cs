using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.cs
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
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Transaction.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeQuaternion type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeVector2 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeVector2Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeVector3Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeVector4 type)
        {
            throw new NotImplementedException();
        }

        public FollowerApply(Types.Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
