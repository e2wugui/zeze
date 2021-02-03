using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Game
{
    public enum TransactionModes
    {
		ExecuteInTheCallerTransaction,
        ExecuteInNestedCall,
	    ExecuteInAnotherThread,
    }

    public class TransactionMode
    {
		public static TaskCompletionSource<int> Run(Func<int> func, string actionName, TransactionModes mode)
		{
			var future = new TaskCompletionSource<int>();
			switch (mode)
			{
				case TransactionModes.ExecuteInTheCallerTransaction:
					future.SetResult(func());
					break;

				case TransactionModes.ExecuteInNestedCall:
					future.SetResult(Game.App.Instance.Zeze.NewProcedure(func, actionName).Call());
					break;

				case TransactionModes.ExecuteInAnotherThread:
					Zeze.Util.Task.Run(() => future.SetResult(Game.App.Instance.Zeze.NewProcedure(func, actionName).Call()), actionName);
					break;
			}
			return future;
		}
	}
}
