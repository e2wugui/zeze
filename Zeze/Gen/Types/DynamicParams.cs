using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.Types
{
	public class DynamicParams
	{
		public Variable Variable { get; set; }
		public string DynamicBase { get; set; }
		public HashSet<string> DynamicBeans { get; } = new();
		public string GetSpecialTypeIdFromBean { get; set; }
		public string CreateBeanFromSpecialTypeId { get; set; }
	}
}
