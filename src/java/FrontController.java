package controller;

import java.io.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

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
            this.scanner.map(this.map, this.controllers, AnnotationGetMapping.class);
            this.scanner.map(this.map, this.controllers, AnnotationPostMapping.class);
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


            // parse requested URL
            String requestedURL = Utils.parseURL(this.projectName, url);

            // Print all controllers
            Utils.printControllers(out, this.controllers);

            // handle requested URL
            Mapping mapping = this.map.get(requestedURL);

            // handle 404 error  
            if (mapping == null) 
            { throw new RequestException("404 NOT FOUND: specified URL not found : " + requestedURL); }

            // invoke methods by reflection
            Object result = Mapping.reflectMethod(mapping, request); // Pass request to extract parameters

            // handle model view 
            Utils.handleModelView(result, out, request, response);
        } 

        catch (RequestException e) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);  // Statut 404
            PrintWriter out = response.getWriter();
            out.println("<html><body><h3>Erreur : " + e.getMessage() + "</h3></body></html>");
            out.close();
        }

        catch (NumberFormatException e) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Statut 400
            PrintWriter out = response.getWriter();
            out.println("<html><body><h3>Erreur de format de nombre : " + e.getMessage() + "</h3></body></html>");
            out.close();
        }

        catch (IllegalArgumentException e) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Statut 400
            PrintWriter out = response.getWriter();
            out.println("<html><body><h3>Erreur d'argument illégal : " + e.getMessage() + "</h3></body></html>");
            out.close();
        }

        catch (Exception e) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);  // Statut 500
            PrintWriter out = response.getWriter();
            out.println("<html><body><h3>Erreur interne du serveur : " + e.getMessage() + "</h3></body></html>");
            out.close();
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
}