@echo off

:: app name and project path 
set "app_name=framework-java"
set "jar_name=framework"
set "root=D:\Studies\ITU\S4\INF - Framework\framework\%app_name%"

:: set paths
set "sourceFolder=%root%\src\java"
set "destinationFolder=%root%\bin"
set "lib=%root%\lib"
set "src=%root%"

::set lib path from web test
set "lib_test=D:\Studies\ITU\S4\INF - Framework\test\web\WEB-INF\lib" 

:: copy all java file to a temporary folder
for /r "%sourceFolder%" %%f in (*.java) do (
    xcopy "%%f" "%root%\temp"
)

:: go to temp and compile all java files
cd "%root%\temp"
javac -d "%destinationFolder%" -cp "%lib%\*" *.java

:: create jar file 
cd "%destinationFolder%"
jar -cvfm "%lib_test%\%jar_name%.jar" "%src%\manifest.txt" *
cd "%src%"

pause