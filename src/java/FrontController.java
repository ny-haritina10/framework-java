package controller;

import java.io.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import com.google.gson.Gson;

import utils.*;
import exceptions.*;

public class FrontController extends HttpServlet {

    private String controllerPackage;
    private String projectName;
    private ControllerScanner scanner;
    private List<Class<?>> controllers;
    private HashMap<String, Mapping> map = new HashMap<String, Mapping>();

    @Override
    public void init() throws ServletException {
        try {
            ServletConfig config = this.getServletConfig();
            ServletContext context = config.getServletContext();

            this.scanner = new ControllerScanner();
            this.controllerPackage = context.getInitParameter("base_package");
            this.projectName = context.getInitParameter("project_name");

            // undefined/empty base_package exception
            if (this.controllerPackage == null || this.controllerPackage.isEmpty()) 
            { throw new BuildException("The 'base_package' parameters is empty or undifined in web.xml"); }

            this.controllers = scanner.findClasses(controllerPackage, AnnotationController.class);

            // not existing base_package
            if (this.controllers.isEmpty()) 
            { throw new BuildException("The folder specified by 'base_package' doesn't exist"); }

            // mapping and scanning
            this.scanner.map(this.map, this.controllers);
        } 
        
        catch (BuildException | RequestException e) {
            System.err.println(e.getMessage());
            throw new ServletException(e);          // rethrow as ServletException to stop the servlet initialization
        }
        
        catch (Exception e) 
        { e.printStackTrace(); }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        try {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURI();
            String methodRequest = request.getMethod();

            String requestedURL = Utils.parseURL(this.projectName, url);
            
            Mapping mapping = findMappingByUrl(requestedURL);

            if (mapping == null) 
            { throw new RequestException("404 NOT FOUND: specified URL not found : " + requestedURL); }

            if (!mapping.getVerb().equals(methodRequest)) 
            { throw new RequestException("405 METHOD NOT ALLOWED: " + methodRequest + " method not allowed for this URL"); }

            String key = methodRequest + ":" + requestedURL;
            mapping = this.map.get(key);

            // Invoke methods by reflection
            Object result = Mapping.reflectMethod(mapping, request);

            // Check if the method has AnnotationRestAPI
            Method method = Class.forName(mapping.getClassName()).getDeclaredMethod(mapping.getMethodName());
            if (method.isAnnotationPresent(AnnotationRestAPI.class)) 
            { Utils.handleRestAPI(result, response); } 
            
            else 
            { Utils.handleModelView(result, out, request, response); }
        } 

        catch (Exception e) 
        { handleException(e, response); }
    }

    private Mapping findMappingByUrl(String url) {
        for (String key : map.keySet()) {
            if (key.endsWith(":" + url)) {
                return map.get(key);
            }
        }
        return null;
    }
   
    private void handleException(Exception e, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        HashMap<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());

        if (e instanceof RequestException) 
        { response.setStatus(HttpServletResponse.SC_NOT_FOUND); } 
        
        else if (e instanceof NumberFormatException || e instanceof IllegalArgumentException) 
        { response.setStatus(HttpServletResponse.SC_BAD_REQUEST); } 
        
        else 
        { response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }

        out.print(gson.toJson(errorResponse));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException 
    {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        processRequest(request, response);
    }
}