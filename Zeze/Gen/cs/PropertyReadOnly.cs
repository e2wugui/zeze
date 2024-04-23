using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class PropertyReadOnly : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine($"{prefix}public long TypeId {{ get; }}");
            sw.WriteLine($"{prefix}public void Encode(ByteBuffer _os_);");
            sw.WriteLine($"{prefix}public bool NegativeCheck();");
            sw.WriteLine($"{prefix}public {bean.Name} Copy();");
            if (bean.Variables.Count > 0)
                sw.WriteLine();
            foreach (Variable var in bean.Variables)
            {
                if (bean.Version.Equals(var.Name))
                    continue; // Version不需要生成访问接口。
                var.VariableType.Accept(new PropertyReadOnly(sw, var, prefix));
            }
        }

        public PropertyReadOnly(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + "ReadOnly " + var.NameUpper1 + " { get; }");
        }

        void WriteProperty(Type type)
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
            sw.WriteLine($"{prefix}public System.Collections.Generic.IReadOnlyDictionary<{keyName},{valueName}> {var.NameUpper1} {{ get; }}");
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

        public void Visit(TypeQuaternion type)
        {
        }

        public void Visit(TypeVector2 type)
        {
        }

        public void Visit(TypeVector2Int type)
        {
        }

        public void Visit(TypeVector3 type)
        {
        }

        public void Visit(TypeVector3Int type)
        {
        }

        public void Visit(TypeVector4 type)
        {
        }

        public void Visit(TypeDecimal type)
        {
            WriteProperty(type);
        }
    }
}
