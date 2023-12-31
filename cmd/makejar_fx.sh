#!/bin/sh

cd ../src
jar cvmf Manifest_fx.txt ../jtcemufx.jar \
  jtcemu/base/*.class \
  jtcemu/platform/fx/*.class \
  jtcemu/platform/fx/base/*.class \
  jtcemu/platform/fx/help/*.class \
  jtcemu/platform/fx/settings/*.class \
  jtcemu/platform/fx/tools/*.class \
  jtcemu/platform/fx/tools/assembler/*.class \
  jtcemu/tools/*.class \
  jtcemu/tools/assembler/*.class \
  z8/*.class \
  help/common/*.htm \
  help/fx/*.htm \
  images/edit/copy.png \
  images/edit/cut.png \
  images/edit/find.png \
  images/edit/paste.png \
  images/edit/settings.png \
  images/file/edit.png \
  images/file/new.png \
  images/file/open.png \
  images/file/print.png \
  images/file/reload.png \
  images/file/reset.png \
  images/file/save.png \
  images/nav/back.png \
  images/nav/home.png \
  images/window/*.png \
  rom/*.bin
