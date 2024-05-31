package utils;

import java.io.*;
import java.lang.reflect.*;

public class Mapping {
    String className;
    String methodName;
    
    public Mapping(String className, String methodName) 
        throws Exception
    {
        this.setClassName(className);
        this.setMethodName(methodName);
    }

    public static Object reflectMethod(Mapping mapping) 
        throws Exception
    {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.newInstance();

            Method method = controllerClass.getDeclaredMethod(mapping.getMethodName(), null);
            method.setAccessible(true);

            Object result = method.invoke(controllerInstance, null);
            return result;    
        } 
        
        catch (Exception e) 
        { e.printStackTrace(); }

        return null;
    }

    public void setClassName(String className) 
        throws Exception
    {
        if (className == null || className.length() == 0 || className.equals(" ")) 
        { throw new Exception("Invalid class name"); }

        this.className = className;
    }

    public void setMethodName(String methodName) 
        throws Exception
    {
        if (methodName == null || methodName.length() == 0 || methodName.equals(" ")) 
        { throw new Exception("Invalid method name"); }

        this.methodName = methodName;
    }

    public String getClassName()
    { return this.className; }

    public String getMethodName() 
    { return this.methodName; }
}