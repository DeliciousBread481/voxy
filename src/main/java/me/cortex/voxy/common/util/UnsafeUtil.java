package me.cortex.voxy.common.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class UnsafeUtil {
    private static final MethodHandle COPY_MEMORY;
    static {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            COPY_MEMORY = MethodHandles.lookup()
                    .unreflect(unsafeClass.getMethod("copyMemory", Object.class, long.class, Object.class, long.class, long.class))
                    .bindTo(unsafe);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    private static final long BYTE_ARRAY_BASE_OFFSET = arrayBaseOffset(byte[].class);
    private static final long SHORT_ARRAY_BASE_OFFSET = arrayBaseOffset(short[].class);
    private static final long LONG_ARRAY_BASE_OFFSET = arrayBaseOffset(long[].class);

    public static void memcpy(long src, long dst, long length) {
        copyMemory(null, src, null, dst, length);
    }



    //Copy the entire length of src to the dst memory where dst is a byte array (source length from dst)
    public static void memcpy(long src, byte[] dst) {
        copyMemory(null, src, dst, BYTE_ARRAY_BASE_OFFSET, dst.length);
    }

    public static void memcpy(long src, int length, byte[] dst) {
        copyMemory(null, src, dst, BYTE_ARRAY_BASE_OFFSET, length);
    }

    public static void memcpy(long src, int length, byte[] dst, int offset) {
        copyMemory(null, src, dst, BYTE_ARRAY_BASE_OFFSET+offset, length);
    }

    //Copy the entire length of src to the dst memory where src is a byte array (source length from src)
    public static void memcpy(byte[] src, long dst) {
        copyMemory(src, BYTE_ARRAY_BASE_OFFSET, null, dst, src.length);
    }

    public static void memcpy(byte[] src, int len, long dst) {
        copyMemory(src, BYTE_ARRAY_BASE_OFFSET, null, dst, len);
    }
    public static void memcpy(short[] src, long dst) {
        copyMemory(src, SHORT_ARRAY_BASE_OFFSET, null, dst, (long) src.length <<1);
    }
    public static void memcpy(long[] src, long dst) {
        copyMemory(src, LONG_ARRAY_BASE_OFFSET, null, dst, (long) src.length <<3);
    }

    private static int arrayBaseOffset(Class<?> arrayClass) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return (int) unsafeClass.getMethod("arrayBaseOffset", Class.class).invoke(unsafe, arrayClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyMemory(Object srcBase, long srcOffset, Object dstBase, long dstOffset, long length) {
        try {
            COPY_MEMORY.invokeExact(srcBase, srcOffset, dstBase, dstOffset, length);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
