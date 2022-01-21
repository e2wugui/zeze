
#include "ToLuaService.h"

namespace demo
{
    class Client : public Zeze::Net::ToLuaService
    {
    public:
        Client() : Zeze::Net::ToLuaService("Client") {}
    };
}
