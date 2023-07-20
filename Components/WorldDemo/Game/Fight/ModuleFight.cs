
namespace Game.Fight
{
    public partial class ModuleFight : AbstractModule
    {
        public void Start(global::Zeze.App app)
        {
        }

        public void Stop(global::Zeze.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessAreYouFightRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as AreYouFight;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
