package utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import mapping.Mapping;
import modelview.ModelView;
import response.FileExportResult;
import verb.VerbAction;

public class Utils {

    public static String parseURL(String projectName, String url) {
        String prefix = "/" + projectName + "/";
        String currentUrl = "";

        if (url.startsWith(prefix)) 
        { currentUrl = "/" + url.substring(prefix.length()); }
        
        System.out.println("given url:" + url);
        System.out.println("Curent URL from parse URL: " + currentUrl);

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
            out.println("Current url: " + currentUrl + " | class name: " + mapping.getClassName());
            out.println("</h2>");
            out.println("<br>");
            out.println("<h3>Available methods:</h3>");

            for (VerbAction verbAction : mapping.getVerbActions()) {
                out.println(verbAction.getVerb() + ": " + verbAction.getMethod() + "<br>");
            }
            
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

    public static void handleRestAPI(Object result, HttpServletResponse response) 
        throws Exception 
    {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            String json = gson.toJson(modelView.getData());

            out.print(json);
        } 
        
        else {
            String json = gson.toJson(result);
            out.print(json);
        }
    }

    public static void handleFileExport(FileExportResult exportResult, HttpServletResponse response)
        throws java.io.IOException 
    {
        response.setContentType(exportResult.getContentType());
        response.setContentLength(exportResult.getFileContent().length);
        
        // set headers 
        response.setHeader("Content-Disposition", "attachment; filename=\"" + exportResult.getFilename() + "\"");

        try (OutputStream outStream = response.getOutputStream()) {
            outStream.write(exportResult.getFileContent());
            outStream.flush();
        }
    }
}