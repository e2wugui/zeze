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
            string valueName = BoxingName.GetBoxingName(type);
            sw.WriteLine(prefix + "private final static class Log_" + var.NamePrivate + " extends Zeze.Transaction.Log1<" + bean.Name + ", " + valueName + "> {");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " self, " + valueName + " value) { super(self, value); }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public long getLogKey() { return this.getBean().getObjectId() + " + var.Id + "; }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public void Commit() { this.getBeanTyped()." + var.NamePrivate + " = this.getValue(); }");
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

        private string GetTemplatParams(Types.Type type)
        {
            if (type is TypeCollection coll)
            {
                return BoxingName.GetBoxingName(coll.ValueType);
            }
            else if (type is TypeMap map)
            {
                return $"{BoxingName.GetBoxingName(map.KeyType)}, {BoxingName.GetBoxingName(map.ValueType)}";
            }
            throw new Exception("Not A Container Type");
        }

        private void WriteCollectionLog(Types.Type type)
        {
            var pn = GetTemplatParams(type);
            var tn = new TypeName();
            type.Accept(tn);

            sw.WriteLine(prefix + $"private final class Log_{var.NamePrivate} extends {tn.nameRaw}.LogV<{pn}> {{");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " host, " + tn.nameCollectionImplement + " value) { super(host, value); }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public long getLogKey() { return getBean().getObjectId() + " + var.Id + "; }");
            sw.WriteLine(prefix + "    public " + bean.Name + " getBeanTyped() { return (" + bean.Name + ")getBean(); }");
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
            sw.WriteLine($"{prefix}    var _typeId_ = bean.getTypeId();");
            sw.WriteLine($"{prefix}    if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)");
            sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.TYPEID;");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}    if (_typeId_ == {real.Value.TypeId}L)");
                sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
            }
            sw.WriteLine($"{prefix}    throw new RuntimeException(\"Unknown Bean! dynamic@{(var.Bean as Bean).FullName}:{var.Name}\");");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_{var.NameUpper1}(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
            }
            sw.WriteLine($"{prefix}    return null;");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }
    }
}
