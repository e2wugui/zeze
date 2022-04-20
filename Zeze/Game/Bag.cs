
namespace Zeze.Game
{
    public class Bag : AbstractBag
    {
        protected override async System.Threading.Tasks.Task<long> ProcessDestroyRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Game.Bag.Destroy;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessMoveRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Game.Bag.Move;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
