@ECHO OFF

cd ..\src
jar cvmf Manifest_se.txt ..\jtcemu.jar ^
  jtcemu\base\*.class ^
  jtcemu\platform\se\*.class ^
  jtcemu\platform\se\audio\*.class ^
  jtcemu\platform\se\base\*.class ^
  jtcemu\platform\se\keyboard\*.class ^
  jtcemu\platform\se\settings\*.class ^
  jtcemu\platform\se\tools\*.class ^
  jtcemu\platform\se\tools\assembler\*.class ^
  jtcemu\platform\se\tools\debugger\*.class ^
  jtcemu\platform\se\tools\hexedit\*.class ^
  jtcemu\tools\*.class ^
  jtcemu\tools\assembler\*.class ^
  z8\*.class ^
  help\common\*.htm ^
  help\se\*.htm ^
  images\debug\*.png ^
  images\edit\*.png ^
  images\file\*.png ^
  images\key\*.png ^
  images\nav\*.png ^
  images\window\*.png ^
  rom\*.bin
cd ..\cmd
