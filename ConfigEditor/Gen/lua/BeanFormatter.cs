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

                foreach (var var in doc.BeanDefine.Variables)
                {
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
                        if (false == var.IsKeyable())
                            continue;
                        if (false == var.PropertiesList.Contains(pid))
                            continue;
                        // 需要确认，如果 var 是 string 时，下面 Index 写法是否正确。
                        sw.WriteLine($"Config.BeansMap{var.NamePinyin}[Config.Beans[{bean.RowIndex + 1}][\"{var.Name}\"]] = Config.Beans[{bean.RowIndex + 1}]");
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
                    sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {{}}");
                }
            }
        }

        public static void GenLoad(StreamWriter sw, string baseTable, VarDefine varDefine, Bean.VarData varData, Property.DataOutputFlags flags)
        {
            switch (varDefine.TypeNow)
            {
                case VarDefine.EType.Date:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = \"{varData.Value}\"");
                    // 先使用string. 看看lua有没有Date类型。
                    break;

                case VarDefine.EType.Double:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {varData.Value}");
                    break;

                case VarDefine.EType.Enum:
                    break; // TODO

                case VarDefine.EType.Float:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {varData.Value}");
                    break;

                case VarDefine.EType.Int:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {varData.Value}");
                    break;

                case VarDefine.EType.Long:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {varData.Value}");
                    break;

                case VarDefine.EType.String:
                    var strValue = (null == varData.Value) ? "" : varData.Value;
                    sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = \"{strValue}\"");
                    break;

                case VarDefine.EType.List:
                    sw.WriteLine($"{baseTable}[\"{varDefine.Name}\"] = {{}}");
                    foreach (var bean in varData.Beans)
                    {
                        var beanVarName = $"{baseTable}[\"{varDefine.Name}\"][{bean.RowIndex + 1}]";
                        sw.WriteLine($"{beanVarName} = {{}}");
                        GenLoad(sw, beanVarName, varDefine.Reference, bean, flags);
                    }
                    break;
            }
        }
    }
}
