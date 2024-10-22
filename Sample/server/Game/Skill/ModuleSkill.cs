
using System.Threading.Tasks;

namespace Game.Skill
{
    public sealed partial class ModuleSkill : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public async Task<Skills> GetSkills(long roleId)
        {
            return new Skills(roleId, await _tSkills.GetOrAddAsync(roleId));
        }
    }
}
