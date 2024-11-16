package upload;

import javax.servlet.http.Part;
import java.io.InputStream;

public class FileUpload {

    private String fileName;
    private String contentType;
    private long size;
    private InputStream inputStream;
    private Part part;

    public FileUpload(Part part) {
        this.part = part;
        this.fileName = extractFileName(part);
        this.contentType = part.getContentType();
        this.size = part.getSize();

        try 
        { this.inputStream = part.getInputStream(); } 
        
        catch (Exception e) 
        { e.printStackTrace(); }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) 
            { return s.substring(s.indexOf("=") + 2, s.length() - 1); }
        }
        return "";
    }

    // save file
    public void saveToDirectory(String directory) 
        throws Exception 
    {
        try 
        { part.write(directory + "/" + fileName); } 
        
        catch (Exception e) 
        { throw new Exception("Error saving file: " + e.getMessage()); }
    }

    // Getters
    public String getFileName() 
    { return fileName; }

    public String getContentType() 
    { return contentType; }

    public long getSize() 
    { return size; }

    public InputStream getInputStream() 
    { return inputStream; }

    public Part getPart() 
    { return part; }
}