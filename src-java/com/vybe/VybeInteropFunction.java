package com.vybe;

import jnr.ffi.annotations.Delegate;
import jnr.ffi.Pointer;

public class VybeInteropFunction {

    public interface VybePtrCallback {
        @Delegate
        void invoke(Pointer data);
    }

    public interface VybePtrIntPtrCallback {
        @Delegate
        void invoke(Pointer p1, Integer i, Pointer p2);
    }

    public interface VybePtrPtrIntPtrCallback {
        @Delegate
        void invoke(Pointer p1, Pointer p2, Integer i, Pointer p3);
    }

    public interface VybeRetIntLongPtrLongPtrCallback {
        @Delegate
        int invoke(long e1, Pointer p1, long e2, Pointer p2);
    }

}
