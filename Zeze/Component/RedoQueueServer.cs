
namespace Zeze.Component
{
    public class RedoQueueServer : AbstractRedoQueueServer
    {
        protected override System.Threading.Tasks.Task<long> ProcessRunTaskRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.RedoQueue.RunTask;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
