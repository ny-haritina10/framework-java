package modelview;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

import utils.*;
import exception.*;
import annotation.*;
import mapping.*;
import scanner.*;
import modelview.*;
import session.*;
import verb.*;
import upload.*;

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
            HashMap<String, Object> data = modelView.getData();

            for (HashMap.Entry<String, Object> entry : data.entrySet()) 
            { request.setAttribute(entry.getKey(), entry.getValue()); }

            RequestDispatcher dispatcher = request.getRequestDispatcher(viewURL);
            dispatcher.forward(request, response);    
        } 
        
        catch (Exception e) 
        { e.printStackTrace(); }
        
    }

    public void add(String variableName, Object variableValue) 
    { this.data.put(variableName, variableValue); }

    public String getViewURL() 
    { return viewURL; }

    public HashMap<String, Object> getData() 
    { return data; }
}