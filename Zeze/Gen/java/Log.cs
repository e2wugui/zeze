using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Log : Visitor
    {
        readonly StreamWriter sw;
        readonly Bean bean;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Log(bean, sw, var, prefix));
            }
        }

        public Log(Bean bean, StreamWriter sw, Variable var, string prefix)
        {
            this.bean = bean;
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
        }

        void WriteLogValue(Types.Type type)
        {
            string valueName = BoxingName.GetBoxingName(type);
            sw.WriteLine(prefix + $"private static final class Log_{var.NamePrivate} extends Zeze.Transaction.Log1<{bean.Name}, {valueName}> {{");
            sw.WriteLine(prefix + $"   public Log_{var.NamePrivate}({bean.Name} bean, int varId, {valueName} value) {{ super(bean, varId, value); }}");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + $"    public void Commit() {{ getBeanTyped().{var.NamePrivate} = this.getValue(); }}");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeBool type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeByte type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeShort type)
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

        public void Visit(TypeFloat type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDouble type)
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

        string GetTemplatParams(Types.Type type)
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

        void WriteCollectionLog(Types.Type type)
        {
            /*
            var pn = GetTemplatParams(type);
            var tn = new TypeName();
            type.Accept(tn);

            sw.WriteLine(prefix + $"private static final class Log_{var.NamePrivate} extends {tn.nameRaw}.LogV<{pn}> {{");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " host, " + tn.nameCollectionImplement + " value) { super(host, value); }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public long getLogKey() { return getBean().getObjectId() + " + var.Id + "; }");
            sw.WriteLine(prefix + "    public " + bean.Name + " getBeanTyped() { return (" + bean.Name + ")getBean(); }");
            sw.WriteLine(prefix + "    @Override");
            sw.WriteLine(prefix + "    public void Commit() { Commit(getBeanTyped()." + var.NamePrivate + "); }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            */
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

        public void Visit(BeanKey type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDynamic type)
        {
            // TypeDynamic 使用写好的类 Zeze.Transaction.DynamicBean，
            // 不再需要生成Log。在这里生成 DynamicBean 需要的两个方法。
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"    // unsupported Log for {type.Name} {var.Name}");
            sw.WriteLine();
            // throw new NotImplementedException();
        }
    }
}
