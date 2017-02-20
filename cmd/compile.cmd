@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jtcemu\*.class
del %SRC_DIR%\jtcemu\audio\*.class
del %SRC_DIR%\jtcemu\base\*.class
del %SRC_DIR%\jtcemu\tools\*.class
del %SRC_DIR%\jtcemu\tools\hexedit\*.class
del %SRC_DIR%\z8\*.class


javac -classpath %SRC_DIR% %1 %2 %3 %SRC_DIR%\jtcemu\Main.java

