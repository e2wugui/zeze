#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module2 {
class BValue : public Zeze::Bean {
public:
    static const int64_t TYPEID = 3672513970861237847LL;

    int S; // com aa

    BValue();
    BValue(int S_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BValue& other);
    BValue& operator=(const BValue& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
