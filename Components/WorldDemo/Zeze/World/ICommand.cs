
using Zeze.Builtin.World;

namespace Zeze.World
{
    public interface ICommand
    {
        Task<long> Handle(BCommand c);
    }
}
