
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
            // 这是Zezex测试服务器里面的一条服务器向客户端发起的rpc请求。
            // 为了不影响Zezex服务器以及关联的单元测试，在这里支持一下。
            // 【WorldDemo并不需要这个模块】
            var p = _p as AreYouFight;
            p.SendResult();
            return Task.FromResult<long>(0);
        }

    }
}
