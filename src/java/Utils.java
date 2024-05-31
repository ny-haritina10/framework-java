package utils;

import java.io.PrintWriter;
import java.lang.reflect.*;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class Utils {

    public static String parseURL(String projectName, String url) {
        String prefix = "/" + projectName + "/";
        String currentUrl = "";

        if (url.startsWith(prefix)) 
        { currentUrl = "/" + url.substring(prefix.length()); }

        return currentUrl;
    }   

    public static void printControllers(PrintWriter out, List<Class<?>> controllers) {
        for (Class<?> controller : controllers) 
        { out.println("Found controller: " + controller.getName() + "<br><br>"); }
    }

    public static void handleRequestedURL(Mapping mapping, PrintWriter out, String currentUrl) {
        if (mapping == null) {
            out.println("<h2>No methods associated with this URL : " + currentUrl + "</h2>");
            return;
        }

        else {
            out.println("<h2>");
            out.println("Current url: " + currentUrl + " | class name: " + mapping.getClassName() + " | method name: " + mapping.getMethodName());
            out.println("</h2>");
            out.println("<br>");
        }
    }

    public static void handleModelView(Object result, PrintWriter out, HttpServletRequest request, HttpServletResponse response) 
        throws Exception
    {
        try {
            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                ModelView.dispatch(modelView, request, response);
            } 
            
            else 
            { out.println("Method result: <h2>" + result + "</h2>"); }   
        } 
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}