package response;

public class FileExportResult {

    private final byte[] fileContent;
    private final String contentType;
    private final String filename;

    public FileExportResult(byte[] fileContent, String contentType, String filename) {

        // validations
        if (fileContent == null) 
        { throw new IllegalArgumentException("File content cannot be null."); }
        
        if (contentType == null || contentType.trim().isEmpty()) 
        { throw new IllegalArgumentException("Content type cannot be null or empty."); }
        
        if (filename == null || filename.trim().isEmpty()) 
        { throw new IllegalArgumentException("Filename cannot be null or empty."); }
        
        this.fileContent = fileContent;
        this.contentType = contentType;
        this.filename = filename;
    }

    public byte[] getFileContent() 
    { return fileContent; }

    public String getContentType() 
    { return contentType; }

    public String getFilename() 
    { return filename; }
}