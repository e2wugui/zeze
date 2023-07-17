using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.Types
{
	public class DynamicParams
	{
		public string Base { get; set; }
		public HashSet<string> Beans { get; } = new();
		public string GetSpecialTypeIdFromBean { get; set; }
		public string CreateBeanFromSpecialTypeId { get; set; }
		public string CreateDataFromSpecialTypeId { get; set; }
		public string GetSpecialTypeIdFromBeanCsharp => Program.Upper1LastName(GetSpecialTypeIdFromBean.Replace("::", "."));
        public string CreateBeanFromSpecialTypeIdCsharp => Program.Upper1LastName(CreateBeanFromSpecialTypeId.Replace("::", "."));
        public string CreateDataFromSpecialTypeIdCsharp => Program.Upper1LastName(CreateDataFromSpecialTypeId.Replace("::", "."));

		public void Compile(ModuleSpace space)
		{
			GetSpecialTypeIdFromBean = ReplaceMacro(GetSpecialTypeIdFromBean);
			CreateBeanFromSpecialTypeId = ReplaceMacro(CreateBeanFromSpecialTypeId);
			CreateDataFromSpecialTypeId = ReplaceMacro(CreateDataFromSpecialTypeId);
        }

		public string ReplaceMacro(string name)
		{
			// TODO 允许预定义组件中的动态Bean关联的实际模块使用新的名字空间。
			// 现在只允许叫"Zeze"。
			// 例子：Zeze.World.World模块在客户端可能需要生成到MySolutionName.World.World。
			// 这里获得这个上下文有点困难，先不实现了。
			return name;
		}
    }
}
