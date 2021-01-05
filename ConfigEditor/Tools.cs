using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public enum CheckNameType
    {
        CheckOnly,
        ShowMsg,
        ThrowExp,
    }

    public class Tools
    {
        private static string ReportError(string msg, CheckNameType type)
        {
            switch (type)
            {
                case CheckNameType.ShowMsg:
                    MessageBox.Show(msg);
                    return msg;

                case CheckNameType.ThrowExp:
                    throw new Exception(msg);

                default:
                    return msg;
            }
        }

        public static string VerifyName(string name, CheckNameType type)
        {
            if (string.IsNullOrEmpty(name))
                return ReportError("name IsNullOrEmpty.", type);

            if (char.IsDigit(name[0]))
                return ReportError("name cannot begin with number.", type);

            switch (name)
            {
                case "bean":
                case "list":
                case "BeanDefine":
                case "variable":
                    return ReportError(name + " is reserved", type);
            }

            foreach (var c in name)
            {
                if (char.IsWhiteSpace(c) || char.IsSymbol(c) || c == '.')
                {
                    return ReportError("char.IsWhiteSpace(c) || char.IsSymbol(c) || c == '.'", type);
                }
            }
            return null;
        }

        public static string ToPinyin(string text)
        {
            StringBuilder sb = new StringBuilder();
            foreach (var c in text)
            {
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_'))
                {
                    sb.Append(c);
                    continue;
                }
                string py = NPinyin.Pinyin.GetPinyin(c);
                if (false == py.Equals(c.ToString()) && py.Length > 0)
                {
                    sb.Append(py.Substring(0, 1).ToUpper() + py.Substring(1));
                }
            }
            return sb.ToString();
        }
    }
}
