package scanner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import verb.VerbAction;

import utils.*;
import exception.*;
import annotation.*;
import mapping.*;
import scanner.*;
import modelview.*;
import session.*;
import verb.*;
import upload.*;


public class ControllerScanner {

    public List<Class<?>> findClasses(String packageName, Class<? extends Annotation> classAnnotation) 
        throws ClassNotFoundException, IOException, BuildException 
    {
        List<Class<?>> controllers = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        assert classLoader != null;

        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        for (File directory : dirs)  
        { controllers.addAll(findClasses(directory, packageName, classAnnotation)); }

        if (controllers.isEmpty()) 
        { throw new BuildException("No controller found in the specified package: " + packageName); }

        return controllers;
    }

    private List<Class<?>> findClasses(File directory, String packageName, Class<? extends Annotation> classAnnotation) 
        throws ClassNotFoundException 
    {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) 
        { return classes; }

        File[] files = directory.listFiles();

        // using assert keywords to avoid if statement
        assert files != null : "files are NULL";

        for (File file : files) {
            if (file.isDirectory()) 
            { classes.addAll(findClasses(file, packageName + "." + file.getName(), classAnnotation)); } 

            else if (file.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if (clazz.isAnnotationPresent(classAnnotation)) 
                { classes.add(clazz); }
            }
        }

        return classes;
    }

    public void map(HashMap<String, Mapping> hash, List<Class<?>> controllers) 
        throws RequestException 
    {
        try {
            for (Class<?> controller : controllers) {
                Method[] methods = controller.getDeclaredMethods();

                for (Method method : methods) {
                    AnnotationURL urlAnnotation = method.getAnnotation(AnnotationURL.class);
                    if (urlAnnotation != null) {
                        String className = controller.getName();
                        String url = urlAnnotation.value();
                        Set<VerbAction> verbActions = new HashSet<>();

                        // handle http verbs by checking annotations
                        if (method.isAnnotationPresent(AnnotationPostMapping.class)) 
                        { verbActions.add(new VerbAction("POST", method.getName())); }

                        if (method.isAnnotationPresent(AnnotationGetMapping.class)) 
                        { verbActions.add(new VerbAction("GET", method.getName())); }

                        // default GET 
                        if (verbActions.isEmpty()) 
                        { verbActions.add(new VerbAction("GET", method.getName())); }

                        Class<?> returnType = method.getReturnType();
                        if (
                            !(returnType.equals(String.class) || 
                            returnType.equals(ModelView.class)) && 
                            !method.isAnnotationPresent(AnnotationRestAPI.class)) 
                        {
                            throw new RequestException(
                                "The method " + method.getName() + " in " + className + 
                                " has returned an invalid type. Returned type: " + returnType.getName());
                        }

                        // check if a Mapping already exists for this url
                        Mapping existingMapping = hash.get(url);

                        // add new verb actions to the existing mapping
                        if (existingMapping != null) 
                        { existingMapping.getVerbActions().addAll(verbActions); } 
                        
                        else {
                            Mapping mapping = new Mapping(className, verbActions);
                            hash.put(url, mapping);
                        }
                    }
                }
            }
        } 
        
        catch (Exception e) {
            e.printStackTrace();
            
            if (e instanceof RequestException) 
            { throw (RequestException) e; }
            
            throw new RuntimeException("Controller mapping error", e);
        }
    }        
}