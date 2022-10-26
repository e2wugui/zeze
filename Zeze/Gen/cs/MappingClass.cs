using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class MappingClass
    {
        public string GenDir { get; }
        public string SrcDir { get; }
        public Types.Bean Bean { get; }

        public MappingClass(string genDir, string srcDir, Types.Bean bean)
        {
            GenDir = genDir;
            SrcDir = srcDir;
            Bean = bean;
        }

        public void Make()
        {
            if (null == Bean.GetFirstDynamicVariable() || false == Bean.RecursiveCheckDynamicCountLessthanOrEqual(1))
            {
                Console.WriteLine($"WARNING: Mapping Class {Bean.FullName}. Invalid Dynamic Variable Count.");
                return;
            }
            List<Types.Bean> inherits = new();
            inherits.Add(Bean);
            var clsName = Bean.MappingClassName(inherits);
            using var sw = Bean.Space.OpenWriter(GenDir, $"Mapping{Bean.Name}.cs");
            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + Bean.Space.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public class Mapping{clsName}");
            sw.WriteLine($"    {{");
            MakeCreateCode(sw, Bean, inherits);
            sw.WriteLine($"    }}");
            sw.WriteLine();
            sw.Write(InheritClass.ToString());
            sw.WriteLine("}");
        }

        public void MakeCreateCode(StreamWriter sw, Types.Bean bean, List<Types.Bean> inherits)
        {
            MakeInheritClass(bean, inherits);

            var clsName = bean.MappingClassName(inherits);
            sw.WriteLine($"        public static {clsName} Create({BuildInherits(inherits)})");
            sw.WriteLine($"        {{");
            var dVar = bean.GetFirstDynamicVariable(); // 一开始已经检查过Dynamic数量，这里直接查询即可。
            List<Bean> subBeans = new();
            if (null == dVar)
            {
                // 没有子类，创建最终类。
                sw.WriteLine($"            return new {clsName}({BuildInherits(inherits, false)});");
            }
            else
            {
                var dDynamic = dVar.VariableType as TypeDynamic;
                foreach (var subCls in dDynamic.DynamicParams.Beans)
                {
                    var beanWithSpecialTypeIdArray = subCls.Split(':');
                    var subBean = Types.Type.Compile(bean.Space, beanWithSpecialTypeIdArray[0]) as Types.Bean;
                    subBeans.Add(subBean);
                }

                // 创建本级switch
                var basep = $"base{inherits.Count - 1}";
                sw.WriteLine($"            switch ({basep}.{dVar.NameUpper1}.Bean.TypeId)");
                sw.WriteLine($"            {{");
                foreach (var subBean in subBeans)
                {
                    sw.WriteLine($"                case {subBean.FullName}.TYPEID:");
                    sw.WriteLine($"                    return Create({BuildInherits(inherits, false)}, ({subBean.FullName}){basep}.{dVar.NameUpper1}.Bean);");
                }
                sw.WriteLine($"            }}");
                sw.WriteLine($"            throw new System.Exception(\"Unkown Dynamic Bean.\");");
            }
            sw.WriteLine($"        }}");
            sw.WriteLine();

            // 深度搜索创建代码。
            foreach (var subBean in subBeans)
            {
                inherits.Add(subBean);
                MakeCreateCode(sw, subBean, inherits);
                inherits.RemoveAt(inherits.Count - 1);
            }
        }

        // 这个应该生成到srcDir，暂时不开放这个功能，先生成到genDir的Create方法的同一个文件中。
        private StringBuilder InheritClass = new();

        private void MakeInheritClass(Bean bean, List<Types.Bean> inherits)
        {
            var clsName = bean.MappingClassName(inherits);
            var inheritsParent = new List<Bean>();
            for (int i = 0; i < inherits.Count - 1; ++i)
                inheritsParent.Add(inherits[i]);
            var baseCls = inheritsParent.Count > 0 ? " : " + inherits[inherits.Count - 1].MappingClassName(inheritsParent) : "";
            InheritClass.Append($"    public class {clsName} {baseCls}\n");
            InheritClass.Append($"    {{\n");
            InheritClass.Append($"        private {bean.FullName} _Bean;\n");
            InheritClass.Append($"        public {clsName}({BuildInherits(inherits)})\n");
            if (inheritsParent.Count > 0)
                InheritClass.Append($"            : base({BuildInherits(inheritsParent, false)})\n");
            InheritClass.Append($"        {{\n");
            InheritClass.Append($"            _Bean = base{inherits.Count - 1};\n");
            InheritClass.Append($"        }}\n");
            InheritClass.Append($"    }}\n");
            InheritClass.Append("\n");
        }

        public string BuildInherits(List<Types.Bean> inherits, bool isParam = true)
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
                    sb.Append($"{dot} base{i}");
                if (first)
                    first = false;
            }
            return sb.ToString();
        }
    }
}
