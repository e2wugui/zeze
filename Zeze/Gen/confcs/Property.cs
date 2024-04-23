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
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            sw.Write(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.Write($" {{ get => {var.NamePrivate}; set {{");
            if (checkNull)
                sw.Write(" if (value == null) throw new System.ArgumentNullException(nameof(value));");
            sw.Write(" " + var.NamePrivate + " = value; }}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
        }

        public void Visit(TypeByte type)
        {
        }

        public void Visit(TypeDouble type)
        {
        }

        public void Visit(TypeInt type)
        {
        }

        public void Visit(TypeLong type)
        {
        }

        public void Visit(TypeBool type)
        {
        }

        public void Visit(TypeBinary type)
        {
        }

        public void Visit(TypeString type)
        {
        }

        public void Visit(TypeList type)
        {
            if (type.ValueType is TypeDynamic dynamicType)
                Visit(dynamicType);
        }

        public void Visit(TypeSet type)
        {
        }

        public void Visit(TypeMap type)
        {
            if (type.ValueType is TypeDynamic dynamicType)
                Visit(dynamicType);
        }

        public void Visit(TypeFloat type)
        {
        }

        public void Visit(TypeShort type)
        {
        }

        public void Visit(TypeDynamic type)
        {
            // var typeName = TypeName.GetName(type);
            // //var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            // sw.WriteLine($"{prefix}public {typeName} {var.NameUpper1} => {var.NamePrivate};");
            // //sw.WriteLine($"{prefix}{typeName}ReadOnly {beanNameReadOnly}.{var.NameUpper1} => {var.NameUpper1};");
            // sw.WriteLine();
            // foreach (Bean real in type.RealBeans.Values)
            // {
            //     string rname = TypeName.GetName(real);
            //     string pname = var.NameUpper1 + "_" + real.Space.Path("_", real.Name);
            //     sw.WriteLine(prefix + "public " + rname + " " + pname);
            //     sw.WriteLine(prefix + "{");
            //     sw.WriteLine(prefix + "    get { return (" + rname + ")" + var.NameUpper1 + ".Bean; }");
            //     sw.WriteLine(prefix + "    set { " + var.NameUpper1 + ".Bean = value; }");
            //     sw.WriteLine(prefix + "}");
            //     sw.WriteLine();
            //     //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
            //     //sw.WriteLine();
            // }

            // foreach (var real in type.RealBeans)
            //     sw.WriteLine($"{prefix}public const long DynamicTypeId{var.NameUpper1}{real.Value.Space.Path("_", real.Value.Name)} = {real.Key};");
            // if (type.RealBeans.Count > 0)
            //     sw.WriteLine();

            var baseType = string.IsNullOrEmpty(type.DynamicParams.Base) ? "Zeze.Util.ConfBean" : type.DynamicParams.Base;
            sw.WriteLine($"{prefix}public static long GetSpecialTypeIdFromBean_{var.Id}({baseType} bean)");
            sw.WriteLine($"{prefix}{{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                if (type.RealBeans.Count > 0)
                {
                    sw.WriteLine($"{prefix}    switch (bean.TypeId)");
                    sw.WriteLine($"{prefix}    {{");
                    // sw.WriteLine($"{prefix}        case Zeze.Util.ConfEmptyBean.TYPEID: return Zeze.Util.ConfEmptyBean.TYPEID;");
                    foreach (var real in type.RealBeans)
                        sw.WriteLine(
                            $"{prefix}        case {real.Value.TypeId}: return {real.Key}; // {real.Value.FullName}");
                    sw.WriteLine($"{prefix}    }}");
                }
                // confcs用于客户端，可能存在老的客户端，此时发现不支持新增bean，不抛出异常。
                sw.WriteLine($"{prefix}    return 0;");
                //sw.WriteLine($"{prefix}    throw new System.Exception(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}: \" + bean.GetType());");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBeanCsharp.Replace("::", ".")}(bean);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static {baseType} CreateBeanFromSpecialTypeId_{var.Id}(long typeId)");
            sw.WriteLine($"{prefix}{{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                if (type.RealBeans.Count > 0)
                {
                    sw.WriteLine($"{prefix}    switch (typeId)");
                    sw.WriteLine($"{prefix}    {{");
                    //sw.WriteLine($"{prefix}        case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
                    foreach (var real in type.RealBeans)
                        sw.WriteLine($"{prefix}        case {real.Key}: return new {real.Value.FullName}();");
                    sw.WriteLine($"{prefix}    }}");
                }
                // confcs用于客户端，可能存在老的客户端，此时发现不支持新增bean，不抛出异常。
                sw.WriteLine($"{prefix}    return null;");
                //sw.WriteLine($"{prefix}    throw new System.Exception(\"Unknown TypeId! dynamic@{((Bean)var.Bean).FullName}:{var.Name}: \" + typeId);");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeIdCsharp}(typeId);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
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

        }
    }
}
