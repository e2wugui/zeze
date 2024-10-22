
using System.Threading.Tasks;
using Zeze.Transaction;
using Zeze.Util;

namespace Game.Fight
{
    public sealed partial class ModuleFight : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public async Task<Fighter> GetFighter(BFighterId fighterId)
        {
            return new Fighter(fighterId, await _tFighters.GetOrAddAsync(fighterId));
        }

        public async Task<long> CalculateFighter(BFighterId fighterId)
        {
            // fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

            Fighter fighter = new Fighter(fighterId, new BFighter());
            switch (fighterId.Type)
            {
                case BFighterId.TypeRole:
                    (await Game.App.Instance.Game_Buf.GetBufs(fighterId.InstanceId)).CalculateFighter(fighter);
                    await Game.App.Instance.Game_Equip.CalculateFighter(fighter);
                    break;
            }
            (await _tFighters.GetOrAddAsync(fighterId)).Assign(fighter.Bean);
            return ResultCode.Success;
        }

        public void StartCalculateFighter(long roleId)
        {
            BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
            Game.App.Instance.Zeze.NewProcedure(async () => await CalculateFighter(fighterId), "CalculateFighter").Execute();
        }
    }
}
