package session;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import annotation.AnnotationGetMapping;
import modelview.ModelView;

public class FormSession {

    private Method lastFormMethod;
    private Object lastControllerInstance;
    private Session session;

    public FormSession(Session session) {
        this.session = session;
    }

    public ModelView invokeLastFormMethod() 
        throws Exception 
    {
        try {
            if (session.get("stored_method") != null && session.get("stored_controller") != null) {
                this.lastFormMethod = (Method) session.get("stored_method");
                this.lastControllerInstance = (Object) session.get("stored_controller");

                this.lastFormMethod.setAccessible(true);
                ModelView mv = (ModelView) this.lastFormMethod.invoke(this.lastControllerInstance);
                System.out.println("invoked method from FormSession: " + mv.getViewURL());

                return mv;
            }

            throw new IllegalStateException("No stored form method found");
        } 
        
        catch (Exception e) {
            throw e;
        }
    }    

    public void storeFormMethod(Method method, Object controllerInstance) {   
        if (method != null && controllerInstance != null) {
            session.add("stored_method", method);
            session.add("stored_controller", controllerInstance);
        }   
    }
}