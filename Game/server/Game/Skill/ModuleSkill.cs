
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

        public Skills GetSkills(long roleId)
        {
            return new Skills(roleId, _tskills.GetOrAdd(roleId));
        }

    }
}
