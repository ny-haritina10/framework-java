package mapping;

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import session.Session;
import verb.VerbAction;

import upload.FileUpload;
import javax.servlet.http.Part;

import utils.*;
import validation.Valid;
import exception.*;
import annotation.*;
import engine.ValidationEngine;
import engine.ValidationResult;
import mapping.*;
import scanner.*;
import modelview.*;
import session.*;
import verb.*;
import upload.*;


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

            // Initialize controller attributes
            Field[] fields = controllerClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(Session.class)) {
                    field.setAccessible(true);
                    field.set(controllerInstance, new Session(request));
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

            if (method == null) {
                throw new NoSuchMethodException("No method found for the verb: " + verb);
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
                    
                    // Set all model attributes
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
                        } else {
                            String paramName = fileUpload.value();
                            Part part = request.getPart(paramName);
                            if (part != null && part.getContentType() != null) {
                                args[i] = new FileUpload(part);
                            }
                        }
                    } catch (Exception e) {
                        throw new Exception("Failed to process file upload: " + e.getMessage());
                    }
                }
                else {
                    throw new Exception("Parameter not annotated properly");
                }
            }

            
            if (!validationResults.isEmpty()) {
                ValidationResult combinedResult = new ValidationResult();
                for (ValidationResult result : validationResults.values()) {
                    combinedResult.addErrors(result.getErrors());
                }
                
                if (method.getReturnType().equals(ModelView.class)) {
                    String requestURL = request.getRequestURI();
                    String viewName = Utils.inferViewName(requestURL);
                    
                    ModelView mv = new ModelView(viewName);
                    mv.add("validationErrors", combinedResult.getErrors());
                    
                    for (int i = 0; i < parameters.length; i++) {
                        if (parameters[i].isAnnotationPresent(AnnotationModelAttribute.class)) {
                            String modelName = parameters[i].getAnnotation(AnnotationModelAttribute.class).value();
                            mv.add(modelName, args[i]);  
                        }
                    }
                    
                    return mv;
                } else {
                    throw new ValidationException(combinedResult);
                }
            }

            // Execute the method if validation passed
            method.setAccessible(true);
            return method.invoke(controllerInstance, args);
        }
        catch (ValidationException e) {
            throw e;
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