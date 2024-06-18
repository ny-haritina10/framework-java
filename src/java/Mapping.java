package utils;

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

public class Mapping {
    private String className;
    private String methodName;

    public Mapping(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public static Object reflectMethod(Mapping mapping, HttpServletRequest request) throws Exception {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            Method method = null;
            for (Method m : controllerClass.getDeclaredMethods()) {
                if (m.getName().equals(mapping.getMethodName())) {
                    method = m;
                    break;
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("Method " + mapping.getMethodName() + " not found in class " + mapping.getClassName());
            }

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];

                if (parameter.isAnnotationPresent(AnnotationRequestParam.class)) {
                    AnnotationRequestParam requestParam = parameter.getAnnotation(AnnotationRequestParam.class);

                    String paramName = requestParam.name();
                    String paramValue = request.getParameter(paramName);

                    args[i] = convertParameterType(paramValue, parameter.getType());
                } 
                
                else if (parameter.isAnnotationPresent(AnnotationModelAttribute.class)) {
                    AnnotationModelAttribute modelAttribute = parameter.getAnnotation(AnnotationModelAttribute.class);
                    String attributeName = modelAttribute.value();

                    Class<?> paramType = parameter.getType();
                    Object model = paramType.getDeclaredConstructor().newInstance();

                    setAllModelAttribute(model, request);
                    args[i] = model;
                }
            }

            method.setAccessible(true);
            return method.invoke(controllerInstance, args);
        } 
        
        catch (Exception e) {
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

        else if (type == java.sql.Date.class) 
        { return java.sql.Date.valueOf(value); }

        return null;
    }

    private static void setAllModelAttribute(Object model, HttpServletRequest request) 
        throws IllegalAccessException 
    {
        Class<?> modelClass = model.getClass();
        Field[] fields = modelClass.getDeclaredFields();

        for (Field field : fields) {
            String paramName = field.getName();
            String paramValue = request.getParameter(paramName);

            if (paramValue != null) {
                field.setAccessible(true);
                field.set(model, convertParameterType(paramValue, field.getType()));
            }
        }
    }
}