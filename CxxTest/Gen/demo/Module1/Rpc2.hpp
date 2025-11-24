#pragma once

#include "zeze/cxx/Rpc.h"
#include "Gen/demo/Module1/BValue.hpp"

namespace demo {
namespace Module1 {
// rpc的注释
class Rpc2 : public Zeze::Net::Rpc<demo::Module1::BValue, demo::Module1::BValue> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = -735856552; // 3559110744
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 7854078040

    virtual int ModuleId() const override {
        return ModuleId_;
    }

    virtual int ProtocolId() const override {
        return ProtocolId_;
    }

    int64_t TypeId() const {
        return TypeId_;
    }
};
}
}
