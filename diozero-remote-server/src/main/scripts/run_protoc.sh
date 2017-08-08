#!/bin/bash

PROTOC_HOME=/cygdrive/d/Apps/protoc-3.3.0-win32

$PROTOC_HOME/bin/protoc --java_out=src/main/java src/main/proto/*.proto2
