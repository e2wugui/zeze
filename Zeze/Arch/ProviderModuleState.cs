using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public sealed class ProviderModuleState
    {
        public long SessionId { get; }
        public int ModuleId { get; }
        public int ChoiceType { get; }
        public bool Dynamic { get; }

        public ProviderModuleState(long sessionId, int moduleId, int choiceType, bool dynamic)
        {
            SessionId = sessionId;
            ModuleId = moduleId;
            ChoiceType = choiceType;
            Dynamic = dynamic;
        }
    }
}
