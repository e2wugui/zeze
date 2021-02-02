
using System;

namespace Zeze.Util
{
    public class Task
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static System.Threading.Tasks.Task Run(Action action, string actionName)
        {
            return System.Threading.Tasks.Task.Run(() =>
            {
                try
                {
                    action();
                }
                catch (Exception ex)
                {
                    logger.Error(ex, actionName);
                }
            });
        }

        public static System.Threading.Tasks.Task Run(Func<int> func, string actionName)
        {
            return System.Threading.Tasks.Task.Run(() =>
            {
                try
                {
                    func();
                }
                catch (Exception ex)
                {
                    logger.Error(ex, actionName);
                }
            });
        }

        public static System.Threading.Tasks.Task Run(Zeze.Transaction.Procedure procdure)
        {
            return System.Threading.Tasks.Task.Run(() =>
            {
                try
                {
                    procdure.Call();
                }
                catch (Exception ex)
                {
                    logger.Error(ex, procdure.ActionName);
                }
            });
        }
    }
}
