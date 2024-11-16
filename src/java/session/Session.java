package session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Session {
    
    private HttpServletRequest request;
    private HttpSession session;

    public Session(HttpServletRequest request) {
        this.session = request.getSession();
    }

    public void add(String key, Object value) 
    { this.session.setAttribute(key, value); }

    public void add(String key, Object[] value) 
    { this.session.setAttribute(key, value); }

    public Object get(String key) 
    { return this.session.getAttribute(key); }

    public void remove(String key)
    { this.session.removeAttribute(key); }
}