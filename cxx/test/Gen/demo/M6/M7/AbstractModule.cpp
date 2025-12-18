#include "AbstractModule.hpp"
#include "Gen/demo/App.h"

namespace demo {
namespace M6 {
namespace M7 {
    const char* AbstractModule::ModuleName = "M7";
    const char* AbstractModule::ModuleFullName = "demo.M6.M7";

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
