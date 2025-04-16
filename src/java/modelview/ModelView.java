package modelview;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

public class ModelView {
    
    String viewURL;
    HashMap<String, Object> data;

    public ModelView(String url) { 
        this.viewURL = url;
        this.data = new HashMap<String, Object>();
    }

    public static void dispatch(ModelView modelView, HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException 
    {
        try {
            String viewURL = modelView.getViewURL();

            // Ensure the URL starts with a slash
            if (!viewURL.startsWith("/")) {
                viewURL = "/" + viewURL;
            }

            System.out.println("view URL after correction: " + viewURL);
            
            HashMap<String, Object> data = modelView.getData();

            for (HashMap.Entry<String, Object> entry : data.entrySet()) 
            { request.setAttribute(entry.getKey(), entry.getValue()); }

            RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(viewURL);
            

            System.out.println("context path: " + request.getContextPath());
            System.out.println("servlet path: " + request.getServletPath());

            dispatcher.forward(request, response);    
        } 
        
        catch (Exception e) 
        { e.printStackTrace(); }
    }

    public void clearData() 
    { this.data.clear(); }

    public void add(String variableName, Object variableValue) 
    { this.data.put(variableName, variableValue); }

    public String getViewURL() 
    { return viewURL; }

    public HashMap<String, Object> getData() 
    { return data; }
}