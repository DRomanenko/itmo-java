#!/bin/bash

OUT=out/production/$MODULE_NAME
ARTIFACTS=java-advanced-2020/artifacts
LIBS=java-advanced-2020/lib
SOL=java-advanced-2020-solutions/java-solutions
MODULE_PATH=ru/ifmo/rain/romanenko/implementor
MODULE_NAME=ru.ifmo.rain.romanenko.implementor

javac -cp "$ARTIFACTS/*" -d "$OUT" --module-path "$ARTIFACTS":"$LIBS" "$SOL"/module-info.java "$SOL"/$MODULE_PATH/*.java
cd "$OUT" || exit
jar -c --file=../_implementor.jar --main-class=$MODULE_NAME.Implementor --module-path="$ARTIFACTS":"$LIBS" module-info.class $MODULE_PATH/*