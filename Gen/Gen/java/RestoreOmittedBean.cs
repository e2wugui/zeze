using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    internal static class RestoreOmittedBean
    {
        internal static string Missing(Variable variable, Bean child, bool isData)
        {
            var type = isData ? javadata.TypeName.GetName(child) : child.FullName;
            var defaults = child is External && variable.Initial.Length > 0 ? $"{type}.ZERO" : $"new {type}()";
            if (child is External)
                return isData ? $"{variable.NamePrivate} = {defaults}" : variable.Setter(defaults);
            var target = isData || variable.Bean.IsRocks ? variable.NamePrivate : variable.Getter;
            return $"{target}.restoreOmittedFields({defaults})";
        }

        internal static void Make(Bean bean, StreamWriter sw, string prefix, bool isData, bool withUnknown)
        {
            if (!bean.RestoreWhenOmitted)
                return;

            var type = isData && !bean.OnlyData ? "Data" : bean.Name;
            sw.WriteLine($"{prefix}public void restoreOmittedFields({type} _defaults_) {{");
            var indent = prefix + "    ";
            if (bean.BaseBean is Bean parent && parent is not External)
                sw.WriteLine($"{indent}super.restoreOmittedFields(_defaults_);");
            foreach (var variable in bean.VariablesIdOrder)
            {
                if (variable.Transient)
                    continue;

                var field = variable.NamePrivate;
                var defaults = $"_defaults_.{field}";
                switch (variable.VariableType)
                {
                    case Bean child when child is not External:
                        var target = isData || bean.IsRocks ? field : variable.Getter;
                        var source = isData || bean.IsRocks ? defaults : $"_defaults_.{variable.Getter}";
                        sw.WriteLine($"{indent}{target}.restoreOmittedFields({source});");
                        break;
                    case TypeSortedMap when isData:
                        sw.WriteLine($"{indent}{field} = new java.util.TreeMap<>();");
                        sw.WriteLine($"{indent}{field}.putAll({defaults});");
                        break;
                    case TypeCollection or TypeMap or TypeSortedMap:
                        sw.WriteLine($"{indent}{field}.clear();");
                        // Only a Data constructor accepts collection values for a nested bean.
                        if (isData)
                        {
                            var add = variable.VariableType is TypeMap ? "putAll" : "addAll";
                            sw.WriteLine($"{indent}{field}.{add}({defaults});");
                        }
                        break;
                    case TypeGTable:
                        if (!isData)
                            sw.WriteLine($"{indent}{field}.clear();");
                        break;
                    case TypeDynamic:
                        sw.WriteLine($"{indent}{field}.assign({defaults});");
                        break;
                    default:
                        var assign = isData ? $"{field} = {defaults}" : variable.Setter($"_defaults_.{variable.Getter}");
                        sw.WriteLine($"{indent}{assign};");
                        break;
                }
            }
            if (withUnknown)
                sw.WriteLine($"{indent}_unknown_ = null;");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }
    }
}
