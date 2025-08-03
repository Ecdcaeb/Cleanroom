package net.minecraftforge.fml.common.eventhandler;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ZZZank
 */
class EventListenerFactory {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static IEventListener bakeListener(Method method, Object instance) {
        try {
            return (IEventListener) method.getDeclaringClass().getMethod("_cleanroom_eventbus_" + method.getName() + "_" + Type.getMethodDescriptor(method).hashCode())
                    .invoke(instance);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
