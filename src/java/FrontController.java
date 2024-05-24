package controller;

import java.io.*;
import java.util.HashMap;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.ServletContext;

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    { 
        try {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURI(); 

            // retrieve the next string after base url "/test"
            String prefix = "/test/";
            String currentUrl = "";
            if (url.startsWith(prefix)) 
            { currentUrl = "/" + url.substring(prefix.length()); }
            
            out.println("<h1>" + " Hello" + "</h1>");
            out.println("<h3> Current URL: " + url + " </h3>");

            for (Class<?> controller : this.controllers) {
                out.println("Found controller: " + controller.getName() + "<br><br>");
            }

            Mapping mapping = this.map.get(currentUrl);
            if (mapping == null) 
            { out.println("<h2>No methods associated with this URL : " + currentUrl + "</h2>"); }

            else {
                out.println("<h2>" + "Current url: " + currentUrl + " | class name: " + mapping.getClassName() + " | method name: " + mapping.getMethodName() + "</h2><br>");
            }
        } 
        
        catch (Exception e) 
        { e.printStackTrace(); }
    }
}