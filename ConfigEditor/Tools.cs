using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor
{
    public class Tools
    {
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
