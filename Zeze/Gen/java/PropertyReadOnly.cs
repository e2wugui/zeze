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
            sw.WriteLine($"{prefix}public long getTypeId();");
            sw.WriteLine($"{prefix}public void Encode(Zeze.Serialize.ByteBuffer _os_);");
            sw.WriteLine($"{prefix}public boolean NegativeCheck();");
            sw.WriteLine($"{prefix}public Zeze.Transaction.Bean CopyBean();");
            sw.WriteLine();
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new PropertyReadOnly(sw, var, prefix));
        }

        public PropertyReadOnly(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        void WriteProperty(Type type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + ";");
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

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + "ReadOnly " + var.Getter + ";");
        }

        public void Visit(BeanKey type)
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

        public void Visit(TypeQuaternion type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new System.NotImplementedException();
        }
    }
}
