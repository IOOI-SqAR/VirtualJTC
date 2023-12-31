@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jtcemu\base\*.class
del %SRC_DIR%\jtcemu\platform\se\*.class
del %SRC_DIR%\jtcemu\platform\se\audio\*.class
del %SRC_DIR%\jtcemu\platform\se\base\*.class
del %SRC_DIR%\jtcemu\platform\se\keyboard\*.class
del %SRC_DIR%\jtcemu\platform\se\settings\*.class
del %SRC_DIR%\jtcemu\platform\se\tools\*.class
del %SRC_DIR%\jtcemu\platform\se\tools\assembler\*.class
del %SRC_DIR%\jtcemu\platform\se\tools\debugger\*.class
del %SRC_DIR%\jtcemu\platform\se\tools\hexedit\*.class
del %SRC_DIR%\jtcemu\tools\*.class
del %SRC_DIR%\jtcemu\tools\assembler\*.class
del %SRC_DIR%\z8\*.class

javac -classpath %SRC_DIR% %* %SRC_DIR%\jtcemu\platform\se\Main.java
