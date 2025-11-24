#pragma once

#include "zeze/cxx/Bean.h"
#include "Gen/demo/Module1/BRemoved2.hpp"

namespace demo {
namespace Module1 {
class BSimple : public Zeze::Bean {
public:
    static const int64_t TYPEID = 4513771153805810055LL;

    int Int_1; // com aa
    int64_t Long2; // com aa
    std::string String3; // com aa
    demo::Module1::BRemoved2 Removed; // com aa

    BSimple();
    BSimple(int Int_1_, int64_t Long2_, const std::string& String3_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BSimple& other);
    BSimple& operator=(const BSimple& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
