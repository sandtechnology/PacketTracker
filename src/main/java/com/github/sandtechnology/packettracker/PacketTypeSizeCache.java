package com.github.sandtechnology.packettracker;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.PUBLIC;

public class PacketTypeSizeCache {
    private static final Map<String, List<Method>> packetTypeSizeMap = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger("PacketTypeSizeCache");

    public static int getPacketSize(Object object) {

        List<Method> methodList = packetTypeSizeMap.computeIfAbsent(object.getClass().getName(), classObj -> {
            List<Method> methods = new ArrayList<>();
            for (Method method : object.getClass().getMethods()) {
                if (!method.isBridge() && !method.isSynthetic() && (method.getModifiers() & PUBLIC) != 0 && method.getParameterCount() == 1 && method.getReturnType() == Void.TYPE) {
                    if (method.getParameterTypes()[0].getName().contains("PacketDataSerializer")) {
                        methods.add(method);
                    }
                }
            }
            return methods;
        });
        if (!methodList.isEmpty()) {
            ByteBuf buf = Unpooled.buffer();
            Object packetDataSerializer = null;
            for (Iterator<Method> iterator = methodList.iterator(); iterator.hasNext(); ) {
                Method method = iterator.next();
                if (packetDataSerializer == null) {
                    try {
                        packetDataSerializer = method.getParameterTypes()[0].getConstructor(ByteBuf.class).newInstance(buf);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
                        iterator.remove();
                    }
                }
                if (packetDataSerializer == null) {
                    continue;
                }
                try {
                    method.invoke(object, packetDataSerializer);
                    return buf.readableBytes();
                } catch (Exception ignored) {
                    iterator.remove();
                }
            }
        }
        logger.warning("Failed to get the size of " + object.getClass().getName());
        return 1;

    }
}
