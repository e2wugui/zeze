using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch.Gen;

namespace Zeze.Gen.java
{
	public class Distribute
	{
		public void GenBean()
		{
			/*
			if (hasDistribute())
			{
				var lastVersion = GetLastVersionBean();
				var curVersion = Gen.GetBean(); // 当前xml的Bean。
				if (null == lastVersion)
				{
					if (curVersion.Name is versioned)
						throw new Exception(); // 第一个版本不能用版本方式命名。
					GenWithoutToPrevious();
					return; // done
				}
				var lastVars = lastVersion.Variables();
				var curVars = curVersion.Variables();
				var (add, remove) = diff(lastVars, curVars);
				var versionDistance = distanceVersion(lastVersion, curVersion); // 版本号差异。
				if (versionDistance > 1)
					throw new Exception("version distance > 1");
				if (add.empty() && remove.empty() && versionDistance > 0)
					throw new Exception("var no change, but bean version changed."); // 这个或者警告即可。

				if (versionDistance == 0)
					throw new Exception("var change, but bean version not change.");
				GenWithToPrevious(add, remove); // 这里包括add,remove都是空的，总是生成旧版兼容。

				return; // done
			}
			var curVersion = Gen.GetBean(); // 当前xml的Bean。
			if (curVersion.Name is versioned)
				throw new Exception(); // 开发期间，不能用版本方式命名。
			GenWithoutToPrevious();
			*/
		}
	}
}
