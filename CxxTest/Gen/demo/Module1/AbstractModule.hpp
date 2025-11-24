#pragma once

#include "zeze/cxx/IModule.h"
#include "Gen/demo/Module1/Protocol3.hpp"
#include "Gen/demo/Module1/Protocol4.hpp"
#include "Gen/demo/Module1/Rpc1.hpp"
#include "Gen/demo/Module1/Rpc2.hpp"
#include "Gen/demo/Module1/RpcOnlyData.hpp"
#include "Gen/demo/Module1/RpcOnlyUseData.hpp"

namespace demo {
    class App;
}
namespace demo {
namespace Module1 {

// module的注释
class AbstractModule  : public Zeze::IModule {
public:
    static const int ModuleId = 1;
    static const char* ModuleName;
    static const char* ModuleFullName;

    virtual int GetId() const override { return ModuleId; }
    virtual const char* GetName() const override { return ModuleName; }
    virtual const char* GetFullName() const override { return ModuleFullName; }

    virtual int64_t ProcessProtocol3(Zeze::Net::Protocol* _p) = 0;
    virtual int64_t ProcessProtocol4(Zeze::Net::Protocol* _p) = 0;
    virtual int64_t ProcessRpc2Request(Zeze::Net::Protocol* _r) = 0;

    demo::App* App;

    AbstractModule(demo::App* app);
    virtual void UnRegister() override;
};
}
}
