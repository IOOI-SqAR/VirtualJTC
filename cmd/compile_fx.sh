#!/bin/sh

SRC_DIR=../src

find $SRC_DIR -name "*.class" -exec rm -f {} \;

javac $* -classpath $SRC_DIR \
  $SRC_DIR/jtcemu/platform/fx/Main.java \
  $SRC_DIR/jtcemu/platform/fx/help/HelpNode.java
