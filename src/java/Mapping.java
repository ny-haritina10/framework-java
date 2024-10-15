package utils;

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import exceptions.*;
import session.Session;
import verb.VerbAction;

public class Mapping {
    
    private String className;
    private Set<VerbAction> verbActions;

    public Mapping(String className, Set<VerbAction> verbActions) {
        this.className = className;
        this.verbActions = verbActions;
    }

    public static Object reflectMethod(Mapping mapping, HttpServletRequest request, String verb) 
        throws Exception 
    {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            Method method = null;

            // init controller attributes
            Field[] fields = controllerClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.getType().equals(Session.class)) {
                    field.setAccessible(true);
                    field.set(controllerInstance, new session.Session(request));
                }
            }
            
            // find method based on the verbs
            for (VerbAction action : mapping.getVerbActions()) {
                if (action.getVerb().equalsIgnoreCase(verb)) {
                    for (Method m : controllerClass.getDeclaredMethods()) {
                        if (m.getName().equals(action.getMethod())) {
                            method = m;
                            break;
                        }
                    }
                    break;
                }
            }

            if (method == null) 
            { throw new NoSuchMethodException("No method found for the verb: " + verb); }

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];

                // Handle session as an argument
                if (parameter.getType().equals(Session.class)) 
                { args[i] = new session.Session(request); }

                else if (parameter.isAnnotationPresent(AnnotationRequestParam.class)) {
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

                else 
                { throw new Exception("ERREUR PARAMETRES NON ANNOTES : ETU002716"); }
            }

            method.setAccessible(true);
            return method.invoke(controllerInstance, args);
        } 
        
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static Object convertParameterType(String value, Class<?> type) 
        throws Exception
    {
        try {
            if (type == String.class) 
            { return value; } 

            else if (type == int.class || type == Integer.class) 
            { return Integer.parseInt(value); } 

            else if (type == double.class || type == Double.class) 
            { return Double.parseDouble(value); } 

            else if (type == java.sql.Date.class) 
            { return java.sql.Date.valueOf(value); }
        }

        catch(Exception e) 
        { throw new RequestException(e.getMessage()); }

        return null;
    }

    private static void setAllModelAttribute(Object model, HttpServletRequest request) 
        throws Exception  
    {
        try {
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
        
        catch (Exception e) 
        { throw e; }
    }

    public static Mapping findMappingByUrl(String url, HashMap<String, Mapping> map) {
        for (String key : map.keySet()) {
            if (key.endsWith(":" + url)) 
            { return map.get(key); }
        }

        return null;
    }

    public String getClassName() 
    { return className; }

    public Set<VerbAction> getVerbActions() 
    { return verbActions; }
}