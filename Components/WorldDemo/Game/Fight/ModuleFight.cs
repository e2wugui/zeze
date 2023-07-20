
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

        protected override System.Threading.Tasks.Task<long> ProcessAreYouFightRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as AreYouFight;
            p.SendResult();
            return Task.FromResult<long>(0);
        }

    }
}
