package com.protocol7.nettyquick.utils;

import java.util.Optional;

public class Opt {
    public static String toString(Optional<?> opt) {
        if (opt.isPresent()) {
            return "[" + opt.get().toString() + "]";
        } else {
            return "[]";
        }
    }

}
