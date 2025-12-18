#pragma once

#include "demo/Module1/ModuleModule1.h"
#include "demo/Module1/Module11/ModuleModule11.h"
#include "demo/M6/ModuleM6.h"
#include "demo/M6/M7/ModuleM7.h"
#include "TaskTest/TaskExt/ModuleTaskExt.h"
#include "demo/TestClient.h"

namespace demo {

class App {
public:
    static App& GetInstance()
    {
        static App staticInstance;
        return staticInstance;
    }

    void Start() {
        CreateService();
        CreateModules();
        StartModules(); // 启动模块，装载配置什么的。
        StartService(); // 启动网络
    }

    void Stop() {
        StopService(); // 关闭网络
        StopModules(); // 关闭模块，卸载配置什么的。
        DestroyModules();
        DestroyServices();
    }

    std::unique_ptr<demo::TestClient> TestClient;

    std::unique_ptr<demo::Module1::ModuleModule1> demo_Module1;
    std::unique_ptr<demo::Module1::Module11::ModuleModule11> demo_Module1_Module11;
    std::unique_ptr<demo::M6::ModuleM6> demo_M6;
    std::unique_ptr<demo::M6::M7::ModuleM7> demo_M6_M7;
    std::unique_ptr<TaskTest::TaskExt::ModuleTaskExt> TaskTest_TaskExt;

    void CreateService() {
        TestClient.reset(new demo::TestClient());
    }

    void CreateModules() {
        demo_Module1.reset(new demo::Module1::ModuleModule1(this));
        demo_Module1_Module11.reset(new demo::Module1::Module11::ModuleModule11(this));
        demo_M6.reset(new demo::M6::ModuleM6(this));
        demo_M6_M7.reset(new demo::M6::M7::ModuleM7(this));
        TaskTest_TaskExt.reset(new TaskTest::TaskExt::ModuleTaskExt(this));
    }

    void DestroyModules() {
        TaskTest_TaskExt.reset(nullptr);
        demo_M6_M7.reset(nullptr);
        demo_M6.reset(nullptr);
        demo_Module1_Module11.reset(nullptr);
        demo_Module1.reset(nullptr);
    }

    void DestroyServices() {
        TestClient.reset(nullptr);
    }

    void StartModules() {
        demo_Module1->Start();
        demo_Module1_Module11->Start();
        demo_M6->Start();
        demo_M6_M7->Start();
        TaskTest_TaskExt->Start();
    }

    void StopModules() {
        if (TaskTest_TaskExt != nullptr)
            TaskTest_TaskExt->Stop();
        if (demo_M6_M7 != nullptr)
            demo_M6_M7->Stop();
        if (demo_M6 != nullptr)
            demo_M6->Stop();
        if (demo_Module1_Module11 != nullptr)
            demo_Module1_Module11->Stop();
        if (demo_Module1 != nullptr)
            demo_Module1->Stop();
    }

    void StartService() {
    }

    void StopService() {
    }
};
}
