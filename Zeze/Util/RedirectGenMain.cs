using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch.Gen;

namespace Zeze.Util
{
	/**
     * 1. 所有需要Redirect支持的内建模块在这里生成代码。
     * 2. 生成的代码提交到代码库中。
     * 3. 该模块创建的时候提供方法，直接创建生成的重载类。不需要用户生成代码。
     * 4. 这个类创建原始内建模块实例，并执行ReplaceModuleInstance生成代码。
     */
	public class RedirectGenMain
	{
		public static void Main(string[] args)
		{
			for (int i = 0; i < args.Length; i++)
			{
				if (args[i].Equals("-GenRedirect"))
					GenModule.Instance.GenRedirect = args[++i];
			}
			if (GenModule.Instance.GenRedirect == null)
			{
				Console.WriteLine("usage: -GenRedirect path_for_genfile_will_place");
				return;
			}

			var app = new AppFake();
			GenModule.Instance.ReplaceModuleInstance(app, new Zeze.Game.Rank());

			Console.WriteLine("==================");
			Console.WriteLine("Gen Redirect Done!");
		}
	}

	class AppFake : AppBase
	{
		public override Application Zeze { get; set; } = null;
	}
}
