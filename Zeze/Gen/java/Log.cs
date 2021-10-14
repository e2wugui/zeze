using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Log : Types.Visitor
    {
        System.IO.StreamWriter sw;
        Types.Bean bean;
        Types.Variable var;
        string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Log(bean, sw, var, prefix));
                sw.WriteLine("");
            }
        }

        public Log(Types.Bean bean, System.IO.StreamWriter sw, Types.Variable var, string prefix)
        {
            this.bean = bean;
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
        }

        private void WriteLogValue(Types.Type type)
        {
            string valueName = TypeName.GetName(type);
            sw.WriteLine(prefix + "private final static class Log_" + var.NamePrivate + " : Zeze.Transaction.Log1<" + bean.Name + ", " + valueName + "> {");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " self, " + valueName + " value) { super(self, value); }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public long getLogKey() { return this.Bean.getObjectId() + " + var.Id + "; }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public void Commit() { this.BeanTyped." + var.NamePrivate + " = this.Value; }");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(BeanKey type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeByte type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeInt type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeLong type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeBool type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeBinary type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeString type)
        {
            WriteLogValue(type);
        }

        private void WriteCollectionLog(Types.Type type)
        {
            var tn = new TypeName();
            type.Accept(tn);

            sw.WriteLine(prefix + "private sealed class Log_" + var.NamePrivate + " : " + tn.name + ".LogV {");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " host, " + tn.nameCollectionImplement + " value) : base(host, value) { }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public long getLogKey() { return Bean.getObjectId() + " + var.Id + "; }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public " + bean.Name + " getBeanTyped() { return (" + bean.Name + ")Bean; }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public void Commit() { Commit(getBeanTyped()." + var.NamePrivate + "); }");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeList type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeSet type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeMap type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeFloat type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeShort type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDynamic type)
        {
            // TypeDynamic 使用写好的类 Zeze.Transaction.DynamicBean，
            // 不再需要生成Log。在这里生成 DynamicBean 需要的两个方法。
            sw.WriteLine($"{prefix}public static long GetSpecialTypeIdFromBean_{var.NameUpper1}(Zeze.Transaction.Bean bean) {{");
            sw.WriteLine($"{prefix}    switch (bean.getTypeId()) {{");
            sw.WriteLine($"{prefix}        case Zeze.Transaction.EmptyBean.TYPEID: return Zeze.Transaction.EmptyBean.TYPEID;");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}        case {real.Value.TypeId}: return {real.Key}; // {real.Value.FullName}");
            }
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}    throw new RuntimeException(\"Unknown Bean! dynamic@{(var.Bean as Bean).FullName}:{var.Name}\");");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_{var.NameUpper1}(long typeId) {{");
            sw.WriteLine($"{prefix}    switch (typeId) {{");
            //sw.WriteLine($"{prefix}        case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}        case {real.Key}: return new {real.Value.FullName}();");
            }
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}    return null;");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }
    }
}
