@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jtcemu\base\*.class
del %SRC_DIR%\jtcemu\platform\fx\*.class
del %SRC_DIR%\jtcemu\platform\fx\base\*.class
del %SRC_DIR%\jtcemu\platform\fx\help\*.class
del %SRC_DIR%\jtcemu\platform\fx\settings\*.class
del %SRC_DIR%\jtcemu\platform\fx\tools\*.class
del %SRC_DIR%\jtcemu\platform\fx\tools\assembler\*.class
del %SRC_DIR%\jtcemu\tools\*.class
del %SRC_DIR%\jtcemu\tools\assembler\*.class
del %SRC_DIR%\z8\*.class

javac -classpath %SRC_DIR% %* ^
  %SRC_DIR%\jtcemu\platform\fx\Main.java ^
  %SRC_DIR%\jtcemu\platform\fx\help\HelpNode.java
