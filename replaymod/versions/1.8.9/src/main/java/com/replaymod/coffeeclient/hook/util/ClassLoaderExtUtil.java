package com.replaymod.coffeeclient.hook.util;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderExtUtil {
    public static void addClassSource(LaunchClassLoader classLoader, URL url) {
        classLoader.addURL(url);
        try {
            ClassLoader parent = classLoader.getParent();
            if (parent instanceof URLClassLoader) {
                Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
                addUrl.invoke(parent, url);
            }
        } catch (Exception e) {
            System.err.println("[CoffeeLoader] Failed to add URL to parent classloader: " + e.getMessage());
        }
    }
}
