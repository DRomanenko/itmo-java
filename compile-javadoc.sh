#!/bin/bash

ARTIFACTS=java-advanced-2020/artifacts
LIBS=java-advanced-2020/lib
LINK=https://docs.oracle.com/en/java/javase/11/docs/api/
SOURCE=java-advanced-2020-solutions/java-solutions
MODULE_NAME=ru.ifmo.rain.romanenko.implementor

javadoc -d "out/_javadoc" -link "$LINK" -sourcepath "$SOURCE" -p "$ARTIFACTS":"$LIBS" "$MODULE_NAME"