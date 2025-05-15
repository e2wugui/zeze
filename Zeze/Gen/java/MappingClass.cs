using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class MappingClass
    {
        public string GenDir { get; }
        public string SrcDir { get; }
        public Bean Bean { get; }

        public MappingClass(string genDir, string srcDir, Bean bean)
        {
            GenDir = genDir;
            SrcDir = srcDir;
            Bean = bean;
        }

        public void Make()
        {
            if (null == Bean.GetFirstDynamicVariable() || false == Bean.RecursiveCheckDynamicCountLessThanOrEqual(1))
            {
                Console.WriteLine($"WARNING: Mapping Class {Bean.FullName}. Invalid Dynamic Variable Count.");
                return;
            }
            List<Bean> inherits = new() { Bean };
            var clsName = Bean.MappingClassName(inherits);
            using var sw = Bean.Space.OpenWriter(GenDir, $"Mapping{clsName}.java");
            if (sw == null)
                return;
            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + Bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine($"public class Mapping{clsName} {{");
            MakeCreateCode(sw, Bean, inherits, true);
            sw.WriteLine($"}}");
        }

        public void MakeCreateCode(StreamWriter sw, Bean bean, List<Bean> inherits, bool isFirst = false)
        {
            MakeInheritClass(bean, inherits);

            var clsName = bean.MappingClassName(inherits);
            if (!isFirst)
                sw.WriteLine();
            sw.WriteLine($"    public static {clsName} create({BuildInherits(inherits)}) {{");
            var dVar = bean.GetFirstDynamicVariable(); // 一开始已经检查过Dynamic数量，这里直接查询即可。
            List<Bean> subBeans = new();
            if (null == dVar)
            {
                // 没有子类，创建最终类。
                sw.WriteLine($"       return new {clsName}({BuildInherits(inherits, false)});");
            }
            else
            {
                var dDynamic = (TypeDynamic)dVar.VariableType;
                foreach (var subCls in dDynamic.DynamicParams.Beans)
                {
                    var beanWithSpecialTypeIdArray = subCls.Split(':');
                    var subBean = Types.Type.Compile(bean.Space, beanWithSpecialTypeIdArray[0]) as Bean;
                    subBeans.Add(subBean);
                }

                // 创建本级switch
                var basep = $"base{inherits.Count - 1}";
                sw.WriteLine($"        var subBean = {basep}.get{dVar.NameUpper1}().getBean();");
                sw.WriteLine($"        var subBeanTypeId = subBean.typeId();");
                foreach (var subBean in subBeans)
                {
                    sw.WriteLine($"        if (subBeanTypeId == {subBean.FullName}.TYPEID)");
                    sw.WriteLine($"            return create({BuildInherits(inherits, false)}, ({subBean.FullName})subBean);");
                }
                sw.WriteLine($"        throw new UnsupportedOperationException(\"Unknown Dynamic Bean: \" + subBeanTypeId);");
            }
            sw.WriteLine($"    }}");

            // 深度搜索创建代码。
            foreach (var subBean in subBeans)
            {
                inherits.Add(subBean);
                MakeCreateCode(sw, subBean, inherits);
                inherits.RemoveAt(inherits.Count - 1);
            }
        }

        private void MakeInheritClass(Bean bean, List<Bean> inherits)
        {
            var clsName = bean.MappingClassName(inherits);
            using var sw = Bean.Space.OpenWriter(SrcDir, clsName + ".java", false); // 全部生成到Root的名字控件下。
            if (sw == null)
                return;
            var inheritsParent = new List<Bean>();
            for (int i = 0; i < inherits.Count - 1; ++i)
                inheritsParent.Add(inherits[i]);
            var baseCls = inheritsParent.Count > 0 ? " extends " + inherits[^1].MappingClassName(inheritsParent) : "";
            sw.WriteLine("package " + Bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine($"public class {clsName} {baseCls} {{");
            sw.WriteLine($"    private {bean.FullName} _Bean;");
            sw.WriteLine($"    public {clsName}({BuildInherits(inherits)}) {{");
            if (inheritsParent.Count > 0)
                sw.WriteLine($"       super({BuildInherits(inheritsParent, false)});");
            sw.WriteLine($"       _Bean = base{inherits.Count - 1};");
            sw.WriteLine($"    }}");
            sw.WriteLine($"}}");
            sw.WriteLine();
        }

        public string BuildInherits(List<Bean> inherits, bool isParam = true)
        {
            var sb = new StringBuilder();
            var first = true;
            for (var i = 0; i < inherits.Count; ++i)
            {
                var dot = first ? "" : ", ";
                var inher = inherits[i];
                if (isParam)
                    sb.Append($"{dot}{inher.FullName} base{i}");
                else
                    sb.Append($"{dot}base{i}");
                if (first)
                    first = false;
            }
            return sb.ToString();
        }
    }
}
