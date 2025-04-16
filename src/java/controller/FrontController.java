package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import annotation.AnnotationController;
import annotation.AnnotationRestAPI;
import exception.BuildException;
import exception.RequestException;
import exception.ValidationException;
import mapping.Mapping;
import response.FileExportResult;
import scanner.ControllerScanner;
import utils.Utils;
import verb.VerbAction;

public class FrontController extends HttpServlet {

    private String controllerPackage;
    private String projectName;
    private ControllerScanner scanner;
    private List<Class<?>> controllers;
    private HashMap<String, Mapping> map = new HashMap<String, Mapping>();

    @Override
    public void init() 
        throws ServletException 
    {        
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
            throw new ServletException(e);
        }
        
        catch (Exception e) 
        { e.printStackTrace(); }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        try {
            // PrintWriter out = response.getWriter();

            String url = request.getRequestURI();
            String methodRequest = request.getMethod();
            String requestedURL = Utils.parseURL(this.projectName, url);
            
            Mapping mapping = this.map.get(requestedURL);

            if (mapping == null) {
                throw new RequestException("404 NOT FOUND: specified URL not found : " + requestedURL);
            }

            VerbAction matchingVerbAction = null;
            for (VerbAction verbAction : mapping.getVerbActions()) {
                if (verbAction.getVerb().equalsIgnoreCase(methodRequest)) {
                    matchingVerbAction = verbAction;
                    break;
                }
            }

            if (matchingVerbAction == null) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);  
                response.setContentType("text/html");  

                PrintWriter errorOut = response.getWriter();
                
                errorOut.println("<h1>500 METHOD NOT ALLOWED</h1>");
                errorOut.println("<hr>");
                errorOut.println("<h4>" + methodRequest + " method is not allowed for the URL: " + requestedURL + "</h4>");

                errorOut.flush();  
                return;  
            }

            // invoke methods by reflection
            Object result = Mapping.reflectMethod(mapping, request, methodRequest);

            // find the method with its parameters
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Method method = null;
            
            // look through all methods to find the matching one
            for (Method m : controllerClass.getDeclaredMethods()) {
                if (m.getName().equals(matchingVerbAction.getMethod())) {
                    method = m;
                    break;
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("Method " + matchingVerbAction.getMethod() + 
                    " not found in " + mapping.getClassName());
            }

            // result handling logic 
            if (result instanceof FileExportResult) 
            { Utils.handleFileExport((FileExportResult) result, response); }

            else if (method.isAnnotationPresent(AnnotationRestAPI.class))
            { Utils.handleRestAPI(result, response); }
            
            else
            {
                PrintWriter out = response.getWriter();
                Utils.handleModelView(result, out, request, response);
            }
        } 
        
        catch (Exception e) {
            e.printStackTrace(); 
            handleException(e, response, request); 
        }
    }

    private void handleException(Exception e, HttpServletResponse response, HttpServletRequest request) 
        throws IOException 
    {
        PrintWriter out = response.getWriter();
        
        if (e instanceof ValidationException) {
            ValidationException ve = (ValidationException) e;

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            
            // Check if it's an AJAX/API request
            if (isAjaxRequest(request)) {
                out.print(ve.toJSON());
            } 
            
            else {
                // for regular requests redirect to a form page
                request.getSession().setAttribute("validationErrors", ve.getFieldErrors());
                response.sendRedirect(request.getHeader("Referer"));
            }
        } 

        else if (e instanceof RequestException) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(createErrorJSON("Not Found", e.getMessage()));
        }

        else if (e instanceof NumberFormatException || e instanceof IllegalArgumentException) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorJSON("Bad Request", e.getMessage()));
        }
        
        else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorJSON("Internal Server Error", e.getMessage()));
        }
    }
    
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
    
    private String createErrorJSON(String type, String message) {
        return String.format("{\"error\": \"%s\", \"message\": \"%s\"}", type, message);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException 
    { processRequest(request, response); }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    { processRequest(request, response); }
}