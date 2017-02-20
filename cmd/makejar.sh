#!/bin/sh

cd ../src
jar cvmf Manifest.txt ../jtcemu.jar \
  jtcemu/*.class \
  jtcemu/audio/*.class \
  jtcemu/base/*.class \
  jtcemu/tools/*.class \
  jtcemu/tools/hexedit/*.class \
  z8/*.class \
  help/*.htm \
  images/edit/*.png \
  images/debug/*.png \
  images/file/*.png \
  images/key/*.png \
  images/nav/*.png \
  images/window/*.png \
  rom/*.bin
