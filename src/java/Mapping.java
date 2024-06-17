package utils;

import java.io.*;
import java.lang.reflect.*;
import java.sql.Date;

import javax.servlet.*;
import javax.servlet.http.*;

public class Mapping {
    String className;
    String methodName;
    
    public Mapping(String className, String methodName) 
        throws Exception
    {
        this.setClassName(className);
        this.setMethodName(methodName);
    }

    public static Object reflectMethod(Mapping mapping, HttpServletRequest request) 
        throws Exception 
    {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.newInstance();
            
            Method method = null;

            // find the mapped method in `mapping`
            for (Method m : controllerClass.getDeclaredMethods()) {
                if (m.getName().equals(mapping.getMethodName())) {
                    method = m;
                    break;
                }
            }

            // inexistant method
            if (method == null) 
            { throw new NoSuchMethodException("Method " + mapping.getMethodName() + " not found in class " + mapping.getClassName()); }

            Parameter[] parameters = method.getParameters();        // retrieve method parameters
            Object[] args = new Object[parameters.length];          


            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];

                // check if param is annoted with `AnnotationRequestParam`
                if (parameter.isAnnotationPresent(AnnotationRequestParam.class)) {

                    AnnotationRequestParam requestParam = parameter.getAnnotation(AnnotationRequestParam.class);

                    String paramName = requestParam.name();     // param name
                    String paramValue = request.getParameter(paramName);        // retrieve param value by request.getParameter()

                    args[i] = convertParameterType(paramValue, parameter.getType());
                }
            }

            method.setAccessible(true);
            return method.invoke(controllerInstance, args);
        }

        catch (Exception e) 
        {
            e.printStackTrace();
            throw e;
        }
    }

    private static Object convertParameterType(String value, Class<?> type) {
        if (type == String.class) 
        { return value; } 
        
        else if (type == int.class || type == Integer.class) 
        { return Integer.parseInt(value); }

        else if (type == double.class || type == Double.class) 
        { return Double.parseDouble(value); }

        else if (type == Date.class)    // java.sql.Date
        { return Date.valueOf(value); }

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