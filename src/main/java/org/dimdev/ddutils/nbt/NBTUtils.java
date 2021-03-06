package org.dimdev.ddutils.nbt;

import net.minecraft.nbt.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class NBTUtils {

    public static NBTTagCompound writeToNBT(Object obj, NBTTagCompound nbt) {
        try {
            Class<?> callingClass = Class.forName(new Exception().getStackTrace()[1].getClassName());
            Class<?> nbtWriter = Class.forName(callingClass.getPackage().getName() + "." + callingClass.getSimpleName() + "NBTWriter");
            Method write = nbtWriter.getMethod("writeToNBT", callingClass, NBTTagCompound.class);
            write.invoke(null, obj, nbt);
            return nbt;
        } catch (ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromNBT(T obj, NBTTagCompound nbt) {
        try {
            Class<?> callingClass = Class.forName(new Exception().getStackTrace()[1].getClassName());
            Class<?> nbtWriter = Class.forName(callingClass.getPackage().getName() + "." + callingClass.getSimpleName() + "NBTWriter");
            Method read = nbtWriter.getMethod("readFromNBT", callingClass, NBTTagCompound.class);
            read.invoke(null, obj, nbt);
        } catch (ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    public static NBTTagCompound writeToNBTNormal(Object obj, NBTTagCompound nbt) {
        try {
            Class<?> objClass = obj.getClass();
            Class<?> nbtWriter = Class.forName(objClass.getPackage().getName() + "." + objClass.getSimpleName() + "NBTWriter");
            Method write = nbtWriter.getMethod("writeToNBT", objClass, NBTTagCompound.class);
            write.invoke(null, obj, nbt);
            return nbt;
        } catch (ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromNBTNormal(T obj, NBTTagCompound nbt) {
        try {
            Class<?> objClass = obj.getClass();
            Class<?> nbtWriter = Class.forName(objClass.getPackage().getName() + "." + objClass.getSimpleName() + "NBTWriter");
            Method read = nbtWriter.getMethod("readFromNBT", objClass, NBTTagCompound.class);
            read.invoke(null, obj, nbt);
        } catch (ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }
}
