using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class PropertyReadOnly : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine($"{prefix}long typeId();");
            sw.WriteLine($"{prefix}void encode(Zeze.Serialize.ByteBuffer _o_);");
            sw.WriteLine($"{prefix}boolean negativeCheck();");
            sw.WriteLine($"{prefix}{bean.Name} copy();");
            if (bean.Variables.Count > 0)
                sw.WriteLine();
            foreach (Variable var in bean.Variables)
            {
                if (bean.Version.Equals(var.Name))
                    sw.WriteLine($"{prefix}long version();");
                else
                    var.VariableType.Accept(new PropertyReadOnly(sw, var, prefix));
            }
        }

        public PropertyReadOnly(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        void WriteProperty(Type type)
        {
            sw.WriteLine(prefix + TypeName.GetName(type) + " " + var.Getter + ";");
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeInt type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeLong type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBinary type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeList type)
        {
            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = type.ValueType.IsNormalBean ? $"PList2ReadOnly<{v}, {v}ReadOnly>" : $"PList1ReadOnly<{v}>";
            sw.WriteLine($"{prefix}Zeze.Transaction.Collections.{t} get{var.NameUpper1}ReadOnly();");
        }

        public void Visit(TypeSet type)
        {
            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = $"Zeze.Transaction.Collections.PSet1ReadOnly<{v}>";
            sw.WriteLine($"{prefix}{t} get{var.NameUpper1}ReadOnly();");
        }

        public void Visit(TypeMap type)
        {
            var k = BoxingName.GetBoxingName(type.KeyType);
            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = type.ValueType.IsNormalBean ? $"PMap2ReadOnly<{k}, {v}, {v}ReadOnly>" : $"PMap1ReadOnly<{k}, {v}>";
            sw.WriteLine($"{prefix}Zeze.Transaction.Collections.{t} get{var.NameUpper1}ReadOnly();");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}{TypeName.GetName(type)}ReadOnly get{var.NameUpper1}ReadOnly();");
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}{TypeName.GetName(type)}ReadOnly get{var.NameUpper1}ReadOnly();");
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                sw.WriteLine($"{prefix}{rname}ReadOnly get{var.NameUpper1}_{real.Space.Path("_", real.Name)}ReadOnly();");
            }
        }

        public void Visit(TypeQuaternion type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeVector2 type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeVector2Int type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeVector3 type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeVector3Int type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeVector4 type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDecimal type)
        {
            WriteProperty(type);
        }
    }
}
