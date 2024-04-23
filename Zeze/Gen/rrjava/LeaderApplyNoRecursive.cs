using System;
using System.IO;

namespace Zeze.Gen.rrjava
{
    public class LeaderApplyNoRecursive : Types.Visitor
    {
        readonly StreamWriter sw;
        readonly Types.Variable var;
        readonly string prefix;

        public static void Make(Types.Bean bean, StreamWriter sw, string prefix)
        {
            foreach (var v in bean.Variables)
            {
                if (!v.Transient && v.VariableType is Types.BeanKey)
                {
                    sw.WriteLine(prefix + "@SuppressWarnings(\"unchecked\")");
                    break;
                }
            }
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + $"public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {{");
            var hasVar = false;
            foreach (var v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                if (!hasVar)
                {
                    hasVar = true;
                    sw.WriteLine(prefix + "    switch (vlog.getVariableId()) {");
                }
                v.VariableType.Accept(new LeaderApplyNoRecursive(v, sw, prefix + "    "));
            }
            if (hasVar)
                sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(Types.TypeBool type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.leaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.leaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.leaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.Bean type)
        {
            // leader apply not need
        }

        public void Visit(Types.BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Types.TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public void Visit(Types.TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).value; break;");
        }

        public LeaderApplyNoRecursive(Types.Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
