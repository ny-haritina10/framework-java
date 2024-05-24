package utils;

public class Mapping {
    String className;
    String methodName;
    
    public Mapping(String className, String methodName) 
        throws Exception
    {
        this.setClassName(className);
        this.setMethodName(methodName);
    }

    public void setClassName(String className) 
        throws Exception
    {
        if (className == null || className.length() == 0 || className.equals(" ")) 
        { throw new Exception("Invalid class name"); }

        this.className = className;
    }

    public void setMethodName(String methodName) 
        throws Exception
    {
        if (methodName == null || methodName.length() == 0 || methodName.equals(" ")) 
        { throw new Exception("Invalid method name"); }

        this.methodName = methodName;
    }

    public String getClassName()
    { return this.className; }

    public String getMethodName() 
    { return this.methodName; }
}