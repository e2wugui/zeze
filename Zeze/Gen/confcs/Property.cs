using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
{
    public class Property : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Property(sw, var, prefix));
        }

        public Property(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            WriteProperty(type);
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            sw.Write(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.Write($" {{ get => {var.NamePrivate}; set {{");
            if (checkNull)
                sw.Write(" if (value == null) throw new System.ArgumentNullException();");
            sw.Write(" " + var.NamePrivate + " = value; }}");
            sw.WriteLine();
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
            WriteProperty(type, true);
        }

        public void Visit(TypeList type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeSet type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeMap type)
        {
            WriteProperty(type);
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
            var typeName = TypeName.GetName(type);
            //var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}public {typeName} {var.NameUpper1} => {var.NamePrivate};");
            //sw.WriteLine($"{prefix}{typeName}ReadOnly {beanNameReadOnly}.{var.NameUpper1} => {var.NameUpper1};");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = var.NameUpper1 + "_" + real.Space.Path("_", real.Name);
                sw.WriteLine(prefix + "public " + rname + " " + pname);
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    get { return (" + rname + ")" + var.NameUpper1 + ".Bean; }");
                sw.WriteLine(prefix + "    set { " + var.NameUpper1 + ".Bean = value; }");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                //sw.WriteLine();
            }

            foreach (var real in type.RealBeans)
                sw.WriteLine($"{prefix}public const long DynamicTypeId{var.NameUpper1}{real.Value.Space.Path("_", real.Value.Name)} = {real.Key};");
            if (type.RealBeans.Count > 0)
                sw.WriteLine();

            sw.WriteLine($"{prefix}public static long GetSpecialTypeIdFromBean_{var.NameUpper1}(Zeze.Util.ConfBean bean)");
            sw.WriteLine($"{prefix}{{");
            sw.WriteLine($"{prefix}    switch (bean.TypeId)");
            sw.WriteLine($"{prefix}    {{");
            sw.WriteLine($"{prefix}        case Zeze.Util.ConfEmptyBean.TYPEID: return Zeze.Util.ConfEmptyBean.TYPEID;");
            foreach (var real in type.RealBeans)
                sw.WriteLine($"{prefix}        case {real.Value.TypeId}: return {real.Key}; // {real.Value.FullName}");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}    throw new System.Exception(\"Unknown Bean! dynamic@{(var.Bean as Bean).FullName}:{var.Name}\");");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Util.ConfBean CreateBeanFromSpecialTypeId_{var.NameUpper1}(long typeId)");
            sw.WriteLine($"{prefix}{{");
            sw.WriteLine($"{prefix}    switch (typeId)");
            sw.WriteLine($"{prefix}    {{");
            //sw.WriteLine($"{prefix}        case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            foreach (var real in type.RealBeans)
                sw.WriteLine($"{prefix}        case {real.Key}: return new {real.Value.FullName}();");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}    return null;");
            sw.WriteLine($"{prefix}}}");
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
    }
}
