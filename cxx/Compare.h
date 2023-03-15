#pragma once

#include "stdint.h"
#include "string"

namespace Zeze {

	class Boolean {
	public:
		static int Compare(bool x, bool y) {
			auto c = (int)x - (int)y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Byte {
	public:
		static int Compare(char x, char y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Short {
	public:
		static int Compare(short x, short y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Integer {
	public:
		static int Compare(int x, int y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Long {
	public:
		static int Compare(int64_t x, int64_t y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Float {
	public:
		static int Compare(float x, float y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class Double {
	public:
		static int Compare(double x, double y) {
			auto c = x - y;
			return c < 0 ? -1 : ((c > 0) ? 1 : 0);
		}
	};

	class String {
	public:
		static int Compare(const std::string & x, const std::string & y) {
			return x.compare(y);
		}
	};
}
