package controller;

import java.io.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import utils.*;

public class FrontController extends HttpServlet {

    private String controllerPackage;
    private ControllerScanner scanner;
    private List<Class<?>> controllers;
    private HashMap<String, Mapping> map = new HashMap<String, Mapping>();

    @Override
    public void init() {
        try {
            ServletConfig config = this.getServletConfig();
            ServletContext context = config.getServletContext();

            this.scanner = new ControllerScanner();
            this.controllerPackage = context.getInitParameter("base_package");

            this.controllers = scanner.findClasses(controllerPackage, AnnotationController.class);
            this.scanner.map(this.map, this.controllers, AnnotationGetMapping.class);
        } 
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        try {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURI();

            // parse requested URL
            String requestedURL = Utils.parseURL("test", url);

            // Print all controllers
            Utils.printControllers(out, this.controllers);

            // handle requested URL
            Mapping mapping = this.map.get(requestedURL);
            Utils.handleRequestedURL(mapping, out, requestedURL);

            // invoke methods by reflection
            Object result = Mapping.reflectMethod(mapping);    
            

            // handle model view 
            Utils.handleModelView(result, out, request, response);
        } 

        catch (Exception e)
        { e.printStackTrace(); }
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