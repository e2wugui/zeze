using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class PropertyReadOnly : Types.Visitor
    {
        System.IO.StreamWriter sw;
        Types.Variable var;
        string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine($"{prefix}public long TypeId {{ get; }}");
            sw.WriteLine($"{prefix}public void Encode(Zeze.Serialize.ByteBuffer _os_);");
            sw.WriteLine($"{prefix}public bool NegativeCheck();");
            sw.WriteLine($"{prefix}public Zeze.Transaction.Bean CopyBean();");
            sw.WriteLine();
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new PropertyReadOnly(sw, var, prefix));
            }
        }

        public PropertyReadOnly(System.IO.StreamWriter sw, Types.Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + "ReadOnly " + var.NameUpper1 + " { get; }");
        }

        private void WriteProperty(Types.Type type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " { get; }");
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
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

        public void Visit(TypeBool type)
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
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            sw.WriteLine(prefix + "public System.Collections.Generic.IReadOnlyList<" + valueName + ">" + var.NameUpper1 + " { get; }");
        }

        public void Visit(TypeSet type)
        {
            var v = TypeName.GetName(type.ValueType);
            var t = $"System.Collections.Generic.IReadOnlySet<{v}>";
            sw.WriteLine($"{prefix}public {t} {var.NameUpper1} {{ get; }}");
        }

        public void Visit(TypeMap type)
        {
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var keyName = TypeName.GetName(type.KeyType);
            sw.WriteLine($"{prefix} public System.Collections.Generic.IReadOnlyDictionary<{keyName},{valueName}> {var.NameUpper1} {{ get; }}");
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}public {TypeName.GetName(type)}ReadOnly {var.NameUpper1} {{ get; }}");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                sw.WriteLine(prefix + "public " + rname + "ReadOnly " + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + " { get; }");
            }
        }
    }
}
