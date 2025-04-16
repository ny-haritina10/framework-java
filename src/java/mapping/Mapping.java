package mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import annotation.AnnotationFileUpload;
import annotation.AnnotationGetMapping;
import annotation.AnnotationModelAttribute;
import annotation.AnnotationRequestParam;
import annotation.Auth;
import annotation.AuthController;
import engine.ValidationEngine;
import engine.ValidationResult;
import exception.RequestException;
import exception.ValidationException;
import mg.jwe.orm.base.BaseModel;
import modelview.ModelView;
import session.FormSession;
import session.Session;
import upload.FileUpload;
import validation.Valid;
import verb.VerbAction;

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
            if (type == int.class) return 0;
            if (type == Integer.class) return null; // Return null for Integer wrapper
            if (type == double.class) return 0.0;
            if (type == Double.class) return null; // Return null for Double wrapper
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
            { 
                // Handle datetime-local format (YYYY-MM-DDThh:mm...)
                if (value.contains("T")) {
                    String adjustedValue = value;
                    if (!adjustedValue.contains(":")) {
                        adjustedValue += ":00"; // Add seconds if missing
                    }
                    if (adjustedValue.split(":").length == 2) {
                        adjustedValue += ":00"; // Add seconds if missing
                    }
                    
                    LocalDateTime localDateTime = LocalDateTime.parse(adjustedValue, 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    
                    return java.sql.Date.valueOf(localDateTime.toLocalDate());
                }

                return java.sql.Date.valueOf(value); 
            }
            else if (type == java.sql.Timestamp.class)
            {
                // Handle datetime-local format for Timestamp
                if (value.contains("T")) {
                    String adjustedValue = value;
                    if (!adjustedValue.contains(":")) {
                        adjustedValue += ":00"; // Add seconds if missing
                    }
                    if (adjustedValue.split(":").length == 2) {
                        adjustedValue += ":00"; // Add seconds if missing
                    }
                    
                    LocalDateTime localDateTime = LocalDateTime.parse(adjustedValue, 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    
                    return java.sql.Timestamp.valueOf(localDateTime);
                }
                // If it's just a date, add default time
                return java.sql.Timestamp.valueOf(value + " 00:00:00");
            }
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
                field.setAccessible(true);
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                // Handle primitive and wrapper types directly
                if (isPrimitiveOrWrapper(fieldType) || fieldType.equals(String.class) ||
                    fieldType.equals(java.sql.Date.class) || fieldType.equals(java.sql.Timestamp.class)) {
                    String paramValue = request.getParameter(fieldName);
                    if (paramValue != null) {
                        field.set(model, convertParameterType(paramValue, fieldType));
                    }
                }
                // Handle nested objects and object references from select options
                else {
                    // First check if there's a direct parameter with the field name (select options case)
                    String paramValue = request.getParameter(fieldName);
                    
                    if (paramValue != null && !paramValue.trim().isEmpty()) {
                        // This is likely from a select option, try to handle it as a foreign key
                        handleForeignKeyField(model, field, paramValue);
                    } 
                    else {
                        // Check if we have nested form fields with dot notation
                        if (!fieldType.getPackage().getName().startsWith("java.") && 
                            !fieldType.isArray() && !Collection.class.isAssignableFrom(fieldType)) {
                            
                            // Get or create instance of the nested class
                            Object nestedObj = field.get(model);
                            if (nestedObj == null) {
                                nestedObj = fieldType.getDeclaredConstructor().newInstance();
                                field.set(model, nestedObj);
                            }
                            
                            // Process nested fields with prefixed parameter names
                            processNestedObject(nestedObj, request, fieldName);
                        }
                    }
                }
            }    
        } 
        catch (Exception e) { 
            throw e; 
        }
    }
    
    private static void handleForeignKeyField(Object model, Field field, String paramValue) 
        throws Exception 
    {
        Class<?> fieldType = field.getType();
        
        // Check if this is a BaseModel subclass (likely a foreign key entity)
        if (BaseModel.class.isAssignableFrom(fieldType)) {
            try {
                // Create a new instance of the foreign key type
                Object foreignKeyInstance = fieldType.getDeclaredConstructor().newInstance();
                
                // Find the id field in the foreign key class
                Field idField = findIdField(fieldType);
                if (idField != null) {
                    idField.setAccessible(true);
                    
                    // Set the ID from the param value (typically from a select option)
                    Object convertedId = convertParameterType(paramValue, idField.getType());
                    idField.set(foreignKeyInstance, convertedId);
                    
                    // Set the foreign key object on the model
                    field.set(model, foreignKeyInstance);
                }
            } catch (Exception e) {
                throw new Exception("Failed to set foreign key field: " + field.getName() + " - " + e.getMessage());
            }
        }
    }
    
    private static Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(mg.jwe.orm.annotations.Id.class)) {
                return field;
            }
        }
        
        // If id is not found in declared fields, try to find it in superclasses
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            return findIdField(superclass);
        }
        
        return null;
    }
    
    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz.equals(Boolean.class) ||
               clazz.equals(Character.class) ||
               clazz.equals(Byte.class) ||
               clazz.equals(Short.class) ||
               clazz.equals(Integer.class) ||
               clazz.equals(Long.class) ||
               clazz.equals(Float.class) ||
               clazz.equals(Double.class);
    }
    
    private static void processNestedObject(Object nestedObj, HttpServletRequest request, String prefix) 
        throws Exception {
        Class<?> nestedClass = nestedObj.getClass();
        Field[] nestedFields = nestedClass.getDeclaredFields();
        
        for (Field nestedField : nestedFields) {
            nestedField.setAccessible(true);
            String nestedFieldName = nestedField.getName();
            Class<?> nestedFieldType = nestedField.getType();
            
            // The parameter name in the form would be something like: 'plane.id', 'plane.name'
            String paramName = prefix + "." + nestedFieldName;
            String paramValue = request.getParameter(paramName);
            
            if (paramValue != null) {
                // Handle primitive/simple fields
                if (isPrimitiveOrWrapper(nestedFieldType) || nestedFieldType.equals(String.class) ||
                    nestedFieldType.equals(java.sql.Date.class) || nestedFieldType.equals(java.sql.Timestamp.class)) {
                    nestedField.set(nestedObj, convertParameterType(paramValue, nestedFieldType));
                }
                // Handle nested objects recursively if needed
                else if (!nestedFieldType.getPackage().getName().startsWith("java.") && 
                         !nestedFieldType.isArray() && !Collection.class.isAssignableFrom(nestedFieldType)) {
                    Object deepNestedObj = nestedField.get(nestedObj);
                    if (deepNestedObj == null) {
                        deepNestedObj = nestedFieldType.getDeclaredConstructor().newInstance();
                        nestedField.set(nestedObj, deepNestedObj);
                    }
                    processNestedObject(deepNestedObj, request, paramName);
                }
            }
        }
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