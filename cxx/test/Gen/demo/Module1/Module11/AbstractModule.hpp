#pragma once

#include "zeze/cxx/IModule.h"

namespace demo {
    class App;
}
namespace demo {
namespace Module1 {
namespace Module11 {

// module的注释
class AbstractModule  : public Zeze::IModule {
public:
    static const int ModuleId = 3;
    static const char* ModuleName;
    static const char* ModuleFullName;

    virtual int GetId() const override { return ModuleId; }
    virtual const char* GetName() const override { return ModuleName; }
    virtual const char* GetFullName() const override { return ModuleFullName; }

    demo::App* App;

    AbstractModule(demo::App* app);
    virtual void UnRegister() override;
};
}
}
}
