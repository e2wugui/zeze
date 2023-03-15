
#include "ToLuaService.h"

namespace demo
{
    class Server : public Zeze.Services.HandshakeServer
    {
    public:
        Server() : Zeze.Services.HandshakeServer("Server") {}
    };
}
