using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.java
{
    public class ParamName : TypeName
    {
		public static string GetParamList(ICollection<Types.Variable> variables)
		{
			StringBuilder plist = new();
			bool first = true;
			foreach (Types.Variable var in variables)
			{
				if (first)
					first = false;
				else
					plist.Append(", ");
				plist.Append(GetName(var.VariableType)).Append(" _").Append(var.Name).Append('_');
			}
			return plist.ToString();
		}
	}
}
