using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.rrcs
{
    public class LeaderApplyNoRecursive : Types.Visitor
    {
        readonly StreamWriter sw;
        readonly Types.Variable var;
        readonly string prefix;

        public static void Make(Types.Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"public override void LeaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    switch (vlog.VariableId)");
            sw.WriteLine(prefix + "    {");
            foreach (var v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new LeaderApplyNoRecursive(v, sw, prefix + "    "));
            }
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(Types.TypeBool type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.LeaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.LeaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.LeaderApplyNoRecursive(vlog); break;");
        }

        public void Visit(Types.Bean type)
        {
            // leader apply not need 
        }

        public void Visit(Types.BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public void Visit(Types.TypeDynamic type)
        {
            throw new NotImplementedException();
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

        public void Visit(Types.TypeDecimal type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)vlog).Value; break;");
        }

        public LeaderApplyNoRecursive(Types.Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
