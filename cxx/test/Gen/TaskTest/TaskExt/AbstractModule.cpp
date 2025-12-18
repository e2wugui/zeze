#include "AbstractModule.hpp"
#include "Gen/demo/App.h"

namespace TaskTest {
namespace TaskExt {
    const char* AbstractModule::ModuleName = "TaskExt";
    const char* AbstractModule::ModuleFullName = "TaskTest.TaskExt";

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
