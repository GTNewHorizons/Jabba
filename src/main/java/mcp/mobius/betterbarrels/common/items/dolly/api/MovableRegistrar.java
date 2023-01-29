package mcp.mobius.betterbarrels.common.items.dolly.api;

import java.util.HashMap;

import mcp.mobius.betterbarrels.BetterBarrels;

public enum MovableRegistrar {

    INSTANCE;

    private HashMap<Class, IDollyHandler> handlers = new HashMap<Class, IDollyHandler>();

    public void registerHandler(String name, IDollyHandler handler) {
        try {
            Class clazz = Class.forName(name);
            if (handlers.containsKey(clazz)) BetterBarrels.log.warn(
                    String.format(
                            "Handler already found for target %s. Overwritting %s with %s",
                            clazz,
                            handlers.get(clazz),
                            handler));
            handlers.put(clazz, handler);
        } catch (ClassNotFoundException e) {
            BetterBarrels.log.warn(String.format("Didn't find class %s to add to the dolly.", name));
        }
    }

    public void registerHandler(Class clazz, IDollyHandler handler) {
        if (handlers.containsKey(clazz)) BetterBarrels.log.warn(
                String.format(
                        "Handler already found for target %s. Overwritting %s with %s",
                        clazz,
                        handlers.get(clazz),
                        handler));
        handlers.put(clazz, handler);
    }

    public IDollyHandler getHandler(Object obj) {
        IDollyHandler retVal = null;

        for (Class clazz : handlers.keySet()) {
            if (clazz.isInstance(obj)) {
                if (retVal != null)
                    throw new RuntimeException(String.format("Multiple handlers to move object %s", obj));
                retVal = handlers.get(clazz);
            }
        }

        return retVal;
    }
}
