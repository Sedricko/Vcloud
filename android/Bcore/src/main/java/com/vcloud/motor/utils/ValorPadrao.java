package com.vcloud.motor.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/** Vcloud: resposta "neutra" para uma pergunta ao sistema que o Android recusou (sem permissão): vazio / falso / zero. */
public final class ValorPadrao {
    private ValorPadrao() {}

    public static Object de(Class<?> t) {
        if (t == void.class || t == Void.class) return null;
        if (t == boolean.class || t == Boolean.class) return Boolean.FALSE;
        if (t == int.class || t == Integer.class) return 0;
        if (t == long.class || t == Long.class) return 0L;
        if (t == float.class || t == Float.class) return 0f;
        if (t == double.class || t == Double.class) return 0d;
        if (t == short.class) return (short) 0;
        if (t == byte.class) return (byte) 0;
        if (t.isArray()) return Array.newInstance(t.getComponentType(), 0);
        if (t == List.class) return new ArrayList<>();
        return null;
    }
}
