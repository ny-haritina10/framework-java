package verb;

import java.util.Objects; 

public class VerbAction {
    
    private String verb;
    private String method;

    public VerbAction(String verb, String method) 
        throws Exception
    {
        this.setVerb(verb);
        this.setMethod(method);
    }


    public String getVerb() 
    { return verb; }

    public void setVerb(String verb) 
        throws Exception
    {
        if (verb == null || verb.equals(""))
        { throw new Exception("Verb can't be empty"); }
         
        this.verb = verb; 
    }

    public String getMethod() 
    { return method; }

    public void setMethod(String method)throws Exception 
    {
        if (method == null || method.equals(""))
        { throw new Exception("Method can't be empty"); }

        this.method = method;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) return false;
        VerbAction other = (VerbAction) obj;

        return Objects.equals(verb, other.getVerb()) || Objects.equals(method, other.getMethod());
    }

    @Override
    public int hashCode()
    { return Objects.hash(verb, method); }
}