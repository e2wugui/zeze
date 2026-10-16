using System.Collections.Concurrent;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    /// <summary>
    /// JSpecify 空安全注解的生成支持。
    /// 生成包统一发 package-info.java（@NullMarked，非空为默认）；生成代码中真正可空的返回
    /// （dynamic 工厂兜底 return null、表格 getReadOnly 转发 TableX.get）标 @Nullable。
    /// 文件头不再压制 "NullableProblems"，生成代码自身保持 null 检查干净。
    /// </summary>
    public static class JSpecify
    {
        public const string NullableImport = "import org.jspecify.annotations.Nullable;";

        // 同一 (space, baseDir) 只写一次；OpenStreamWriter 自身也按全路径去重，这里是省掉重复开销。
        static readonly ConcurrentDictionary<string, bool> PackageInfos = new();

        /// <summary>
        /// 为生成目录下的包写 package-info.java（@NullMarked）。
        /// baseDir 必须是生成目录（GenDir/CommonDir）；SrcDir 是用户骨架领地，不要传进来。
        /// 必须走 Program.OpenStreamWriter 注册进 Outputs，否则会被 GenDir 的过期文件清理删除。
        /// </summary>
        public static void GenPackageInfo(ModuleSpace space, string baseDir)
        {
            string fullDir = Path.GetFullPath(space.GetFullPath(baseDir));
            if (false == PackageInfos.TryAdd(fullDir, true))
                return;

            var sw = Program.OpenStreamWriter(Path.Combine(fullDir, "package-info.java"));
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("@NullMarked");
            sw.WriteLine("package " + space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import org.jspecify.annotations.NullMarked;");
        }

        /// <summary>
        /// 在限定类型名的最后一段上放 @Nullable，返回可直接作为返回类型使用的字符串。
        /// javac 规则：type-use 注解不能写在整个限定名之前（@Nullable a.b.C 非法），
        /// 必须贴在最后一段（a.b.@Nullable C）；简单名则直接前置。
        /// </summary>
        public static string NullableOnLastName(string qualifiedType)
        {
            int genericStart = qualifiedType.IndexOf('<');
            int lastDot = genericStart >= 0
                ? qualifiedType.LastIndexOf('.', genericStart)
                : qualifiedType.LastIndexOf('.');
            if (lastDot < 0)
                return "@Nullable " + qualifiedType;
            return qualifiedType.Substring(0, lastDot + 1) + "@Nullable " + qualifiedType.Substring(lastDot + 1);
        }

        /// <summary>
        /// bean 文件是否需要 import org.jspecify.annotations.Nullable：
        /// ① 顶层普通 bean（Base 为空）——unknown 字段支持生成 byte @Nullable [] _unknown_ 与 unknown()；
        /// ② MapKeyTypes 非空——生成 @Nullable 的 __zeze_map_key__ 与 mapKey()；
        /// ③ 存在 switch 变体的 dynamic 变量——工厂兜底 return null 标 @Nullable。
        /// 转发变体（DynamicParams 指定目标方法）不标 @Nullable，不算数。
        /// OnlyData bean 走 javadata 路径：没有 unknown 字段和 mapKey，只有 toData 兜底用 @Nullable。
        /// </summary>
        public static bool BeanNeedsNullableImport(Bean bean)
        {
            if (bean.GenEquals || Program.isOnlyData(bean) || Program.isData(bean))
                return true;
            // 构造器为引用类型的 null 实参创建默认值（不同于 setter 的非空防御契约）。
            foreach (var v in bean.Variables)
            {
                if (!bean.Version.Equals(v.Name) && !v.VariableType.IsJavaPrimitive)
                    return true;
            }
            if (Program.isOnlyData(bean))
            {
                foreach (var v in bean.Variables)
                {
                    var dy = v.VariableType as TypeDynamic
                             ?? (v.VariableType as TypeMap)?.ValueType as TypeDynamic
                             ?? (v.VariableType as TypeCollection)?.ValueType as TypeDynamic;
                    if (dy != null && string.IsNullOrEmpty(dy.DynamicParams.CreateDataFromSpecialTypeId))
                        return true;
                }
                return false;
            }

            if (bean.Base == "" || bean.MapKeyTypes.Count > 0)
                return true;

            foreach (var v in bean.Variables)
            {
                var dy = v.VariableType as TypeDynamic
                         ?? (v.VariableType as TypeMap)?.ValueType as TypeDynamic
                         ?? (v.VariableType as TypeSortedMap)?.ValueType as TypeDynamic
                         ?? (v.VariableType as TypeCollection)?.ValueType as TypeDynamic
                         ?? (v.VariableType as TypeGTable)?.ValueType as TypeDynamic;
                if (dy != null && string.IsNullOrEmpty(dy.DynamicParams.CreateBeanFromSpecialTypeId))
                    return true;
            }
            return false;
        }
    }
}
