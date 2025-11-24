#include "AbstractModule.hpp"
#include "Gen/demo/App.h"

namespace demo {
namespace Module1 {
namespace Module11 {
    const char* AbstractModule::ModuleName = "Module11";
    const char* AbstractModule::ModuleFullName = "demo.Module1.Module11";

    AbstractModule::AbstractModule(demo::App* app)
    {
        App = app;
        // register protocol factory and handles
    }

    void AbstractModule::UnRegister()
    {
    }
}
}
}
