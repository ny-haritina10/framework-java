package utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.*;

import utils.*;

public class ControllerScanner {

    public List<Class<?>> findClasses(String packageName, Class<?> classAnnotation) 
        throws ClassNotFoundException, IOException 
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

        for (File directory : dirs) {
            controllers.addAll(findClasses(directory, packageName, classAnnotation));
        }

        return controllers;
    }

    private List<Class<?>> findClasses(File directory, String packageName, Class<?> classAnnotation) 
        throws ClassNotFoundException 
    {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName(), classAnnotation));
            } 
            
            else if (file.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if (clazz.isAnnotationPresent((Class<? extends Annotation>) classAnnotation)) {
                    classes.add(clazz);
                }
            }
        }

        return classes;
    }

    public void map(HashMap<String, Mapping> hash, List<Class<?>> controllers, Class<?> classAnnotation) {
        try {
            for (Class<?> controller : controllers) {
                Method[] methods = controller.getDeclaredMethods();
    
                for (Method method : methods) {
                    if (method.isAnnotationPresent(AnnotationGetMapping.class)) {
                        String className = controller.getName();
                        String methodName = method.getName();
                        String url = method.getAnnotation(AnnotationGetMapping.class).url();
    
                        Mapping mapping = new Mapping(className, methodName);
                        hash.put(url, mapping);
                    }
                }
            }
        } 
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}