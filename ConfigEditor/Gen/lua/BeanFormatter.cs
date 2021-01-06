using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace ConfigEditor.Gen.lua
{
    public class BeanFormatter
    {
        public static void Gen(string srcHome, Document doc, Property.DataOutputFlags flags)
        {
            using (StreamWriter sw = doc.OpenStreamWriter(srcHome, ".lua"))
            {
                sw.WriteLine($"-- auto generated.");
                sw.WriteLine();
                sw.WriteLine($"local Config = {{}}");
                sw.WriteLine();
                sw.WriteLine($"Config.Beans = {{}}");

                if (false == doc.Main.PropertyManager.Properties.TryGetValue(Property.Id.PName, out var pid))
                    throw new Exception("Property.Id miss!");

                // gen enum
                SortedDictionary<string, EnumDefine> enums = new SortedDictionary<string, EnumDefine>();
                doc.BeanDefine.ForEach((BeanDefine bd) =>
                {
                    foreach (var e in bd.EnumDefines.Values)
                        enums.Add(e.FullNamePinyin().Replace('.', '_'), e);
                    return true;
                });
                sw.WriteLine($"Config.Enums = {{}}");
                foreach (var e in enums)
                {
                    sw.WriteLine($"Config.Enums.{e.Key} = {{}}");
                    foreach (var v in e.Value.ValueMap.Values)
                    {
                        sw.WriteLine($"Config.Enums.{e.Key}.{v.NamePinyin} = {v.Value}");
                    }
                }
                // gen map if need
                foreach (var var in doc.BeanDefine.Variables)
                {
                    if (0 == (var.DataOutputFlags & flags))
                        continue;
                    if (false == var.IsKeyable())
                        continue;
                    if (false == var.PropertiesList.Contains(pid))
                        continue;
                    sw.WriteLine($"Config.BeansMap{var.NamePinyin} = {{}}");
                }
                sw.WriteLine();
                foreach (var bean in doc.Beans)
                {
                    sw.WriteLine($"Config.Beans[{bean.RowIndex + 1}] = {{}}");
                    GenLoad(sw, $"Config.Beans[{bean.RowIndex + 1}]", doc.BeanDefine, bean, flags);
                    foreach (var var in doc.BeanDefine.Variables)
                    {
                        if (0 == (var.DataOutputFlags & flags))
                            continue;
                        if (false == var.IsKeyable())
                            continue;
                        if (false == var.PropertiesList.Contains(pid))
                            continue;
                        // 需要确认，如果 var 是 string 时，下面 Index 写法是否正确。
                        sw.WriteLine($"Config.BeansMap{var.NamePinyin}[Config.Beans[{bean.RowIndex + 1}].{var.NamePinyin}] = Config.Beans[{bean.RowIndex + 1}]");
                    }
                    sw.WriteLine();
                }
                sw.WriteLine();
                sw.WriteLine($"return Config");
            }
        }

        public static void GenLoad(StreamWriter sw, string baseTable, BeanDefine beanDefine, Bean bean, Property.DataOutputFlags flags)
        {
            foreach (var varDefine in beanDefine.Variables)
            {
                if (0 == (varDefine.DataOutputFlags & flags))
                    continue;
                if (bean.VariableMap.TryGetValue(varDefine.Name, out var varData))
                {
                    GenLoad(sw, baseTable, varDefine, varData, flags);
                }
                else if (VarDefine.EType.List == varDefine.TypeNow)
                {
                    sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = {{}}");
                }
                else if (false == string.IsNullOrEmpty(varDefine.Default))
                {
                    sw.WriteLine($"{baseTable}.{varDefine.NamePinyin}\"] = {GetDefaultInitialize(varDefine)}");
                }
            }
        }

        private static string GetDefaultInitialize(VarDefine var)
        {
            switch (var.TypeNow)
            {
                case VarDefine.EType.String:
                case VarDefine.EType.Date:
                    return $"\"{var.Default}\"";

                case VarDefine.EType.Int:
                case VarDefine.EType.Long:
                case VarDefine.EType.Float:
                case VarDefine.EType.Double:
                    return var.Default;

                case VarDefine.EType.Enum:
                    return $"Config.Enums.{var.FullNamePinyin().Replace('.', '_')}.{var.DefaultPinyin}]";

                default:
                    throw new Exception("unknown type");
            }
        }

        public static void GenLoad(StreamWriter sw, string baseTable, VarDefine varDefine, Bean.VarData varData, Property.DataOutputFlags flags)
        {
            switch (varDefine.TypeNow)
            {
                case VarDefine.EType.Date:
                    // 先使用string. 看看lua怎么用。
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = \"{varData.Value}\"");
                    else if (false == string.IsNullOrEmpty(varDefine.Default))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = \"{varDefine.Default}\"");
                    break;

                case VarDefine.EType.Int:
                case VarDefine.EType.Float:
                case VarDefine.EType.Double:
                case VarDefine.EType.Long:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = {varData.Value}");
                    else if (false == string.IsNullOrEmpty(varDefine.Default))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = {varDefine.Default}");
                    break;

                case VarDefine.EType.Enum:
                    var enumClassName = varDefine.FullNamePinyin().Replace('.', '_');
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = Config.Enums.{enumClassName}.{varData.ValuePinyin}");
                    else if (false == string.IsNullOrEmpty(varDefine.Default))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = Config.Enums.{enumClassName}.{varDefine.DefaultPinyin}");
                    break;

                case VarDefine.EType.String:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = \"{varData.Value}\"");
                    else if (false == string.IsNullOrEmpty(varDefine.Default))
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = \"{varDefine.Default}\"");
                    else
                        sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = \"\"");
                    break;

                case VarDefine.EType.List:
                    sw.WriteLine($"{baseTable}.{varDefine.NamePinyin} = {{}}");
                    foreach (var bean in varData.Beans)
                    {
                        var beanVarName = $"{baseTable}.{varDefine.NamePinyin}[{bean.RowIndex + 1}]";
                        sw.WriteLine($"{beanVarName} = {{}}");
                        GenLoad(sw, beanVarName, varDefine.Reference, bean, flags);
                    }
                    break;
            }
        }
    }
}
