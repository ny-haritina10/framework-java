package mapping;

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import session.FormSession;
import session.Session;
import verb.VerbAction;

import upload.FileUpload;
import javax.servlet.http.Part;

import validation.Valid;
import exception.*;
import annotation.*;
import engine.ValidationEngine;
import engine.ValidationResult;
import modelview.*;


public class Mapping {
    
    private String className;
    private Set<VerbAction> verbActions;

    public Mapping(String className, Set<VerbAction> verbActions) {
        this.className = className;
        this.verbActions = verbActions;
    }

    @SuppressWarnings("rawtypes")
    public static Object reflectMethod(Mapping mapping, HttpServletRequest request, String verb) 
        throws Exception 
    {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            Method method = null;

            // session to store errors
            Session sess = new Session(request);
            FormSession session = new FormSession(sess);

            // Initialize controller attributes
            Field[] fields = controllerClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(Session.class)) {
                    field.setAccessible(true);
                    field.set(controllerInstance, sess);
                }
            }
            
            // Find method based on the verbs
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


            if (verb.equalsIgnoreCase("GET") && method != null && method.isAnnotationPresent(AnnotationGetMapping.class)) 
            { session.storeFormMethod(method, controllerInstance); }

            // check method annotation 
            if (!isAccessAllowed(method, sess)) {
                ModelView mv = new ModelView("not-authenticated.jsp");
                mv.add("message", "User not authenticated");

                return mv;
            }

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            Map<Integer, ValidationResult> validationResults = new HashMap<>();

            // Process parameters and perform validation
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];

                // Handle different parameter types
                if (parameter.getType().equals(Session.class)) {
                    args[i] = new Session(request);
                }

                else if (parameter.isAnnotationPresent(AnnotationRequestParam.class)) {
                    AnnotationRequestParam requestParam = parameter.getAnnotation(AnnotationRequestParam.class);
                    String paramName = requestParam.name();
                    String paramValue = request.getParameter(paramName);

                    args[i] = convertParameterType(paramValue, parameter.getType());
                }

                else if (parameter.isAnnotationPresent(AnnotationModelAttribute.class)) {
                    AnnotationModelAttribute modelAttribute = parameter.getAnnotation(AnnotationModelAttribute.class);
                    Class<?> paramType = parameter.getType();
                    Object model = paramType.getDeclaredConstructor().newInstance();
                    
                    setAllModelAttribute(model, request);
                    
                    // Perform validation if @Valid is present
                    if (parameter.isAnnotationPresent(Valid.class) || paramType.isAnnotationPresent(Valid.class)) {
                        ValidationResult validationResult = ValidationEngine.validate(model);

                        if (!validationResult.isValid()) {
                            validationResults.put(i, validationResult);
                        }
                    }
                    
                    args[i] = model;
                }

                else if (parameter.isAnnotationPresent(AnnotationFileUpload.class)) {
                    AnnotationFileUpload fileUpload = parameter.getAnnotation(AnnotationFileUpload.class);
                    boolean isMultiple = fileUpload.multiple();
                    
                    try {
                        if (isMultiple) {
                            Collection<Part> parts = request.getParts();
                            List<FileUpload> files = new ArrayList<>();
                            for (Part part : parts) {
                                if (part.getContentType() != null) {
                                    files.add(new FileUpload(part));
                                }
                            }
                            args[i] = files;
                        } 
                        
                        else {
                            String paramName = fileUpload.value();
                            Part part = request.getPart(paramName);
                            if (part != null && part.getContentType() != null) {
                                args[i] = new FileUpload(part);
                            }
                        }
                    } 
                    
                    catch (Exception e) {
                        throw new Exception("Failed to process file upload: " + e.getMessage());
                    }
                }

                else {
                    throw new Exception("Parameter not annotated properly");
                }
            }   

            // fields validation failed
            if (!validationResults.isEmpty()) {
                ValidationResult combinedResult = new ValidationResult();
                for (ValidationResult result : validationResults.values()) {
                    combinedResult.addErrors(result.getErrors());
                }
            
                if (session != null) {
                    try {

                        // retrieve the last methods 
                        ModelView mv = session.invokeLastFormMethod();
                        mv.add("validationErrors", combinedResult);

                        
                        for (int i = 0; i < parameters.length; i++) {
                            if (parameters[i].isAnnotationPresent(AnnotationModelAttribute.class)) {
                                String modelName = parameters[i].getAnnotation(AnnotationModelAttribute.class).value();
                                mv.add(modelName, args[i]);
                            }
                        }

                        return mv;
                    } 
                    
                    catch (IllegalStateException e) {
                        e.printStackTrace();
                        throw new ValidationException(combinedResult);
                    }
                }
            }
            
            // Execute the method if validation passed
            method.setAccessible(true);
            request.removeAttribute("validationErrors");

            return method.invoke(controllerInstance, args);
        }

        catch (ValidationException e) 
        { throw e; }

        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static boolean isAccessAllowed(Method method, Session sess) {
        Class<?> controllerClass = method.getDeclaringClass();
        
        // controller-level auth
        if (controllerClass.isAnnotationPresent(AuthController.class)) {
            if (!isAuthenticated(sess)) 
            { return false; }

            AuthController controllerAuth = controllerClass.getAnnotation(AuthController.class);
            if (controllerAuth.roles().length > 0 && !controllerAuth.roles()[0].equals(Void.class)) {
                if (!hasRequiredRole(sess, controllerAuth.roles())) 
                { return false; }
            }
        }

        // method-level auth
        if (method.isAnnotationPresent(Auth.class)) {
            if (!isAuthenticated(sess)) 
            { return false; }

            Auth methodAuth = method.getAnnotation(Auth.class);
            if (methodAuth.roles().length > 0 && !methodAuth.roles()[0].equals(Void.class)) {
                if (!hasRequiredRole(sess, methodAuth.roles())) 
                { return false; }
            }
        }

        return true;
    }

    private static boolean isAuthenticated(Session sess) {
        if (sess.get("authenticated") == null) {
            return false;
        }
        return (boolean) sess.get("authenticated");
    }

    private static boolean hasRequiredRole(Session sess, Class<?>[] allowedRoles) {
        Class<?> sessionRole = (Class<?>) sess.get("profile");
        if (sessionRole == null) 
        { return false; }

        for (Class<?> role : allowedRoles) {
            if (role.equals(sessionRole)) 
            { return true; }
        }

        return false;
    }

    private static Object convertParameterType(String value, Class<?> type) 
        throws Exception 
    {        
        // null or empty values
        if (value == null || value.trim().isEmpty()) {
            if (!type.isPrimitive()) 
            { return null; }
            
            // return their default values
            if (type == int.class || type == Integer.class) return 0;
            if (type == double.class || type == Double.class) return 0.0;
            if (type == boolean.class) return false;
            
            throw new RequestException("Cannot convert empty value to primitive type: " + type.getName());
        }

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
        
        catch (Exception e) 
        { throw new RequestException("Invalid value for type " + type.getName() + ": " + value); }

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