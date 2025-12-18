#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
class BOnlyData : public Zeze::Bean {
public:
    static const int64_t TYPEID = -8136515036661024621LL;

    int S; // com aa

    BOnlyData();
    BOnlyData(int S_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BOnlyData& other);
    BOnlyData& operator=(const BOnlyData& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
