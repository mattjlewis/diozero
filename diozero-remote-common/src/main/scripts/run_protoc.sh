#!/bin/bash

PROTOC_HOME=$HOME/Java/protoc-3.13.0
#NANOPB_HOME=/cygdrive/d/Apps/nanopb-0.4.1-windows-x86

$PROTOC_HOME/bin/protoc --java_out=src/main/java src/main/proto/*.proto
#$NANOPB_HOME/generator-bin/protoc --nanopb_out=src/main/c src/main/proto/diozero.proto
