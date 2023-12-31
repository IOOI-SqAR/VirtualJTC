@ECHO OFF

REM
REM Die Umgebungsvariable PATH_TO_FX
REM muss auf das lib-Verzeichnis von OpenJFX zeigen!
REM

set SRC_DIR=..\src

java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.web ^
  -classpath %SRC_DIR% jtcemu.platform.fx.Main
