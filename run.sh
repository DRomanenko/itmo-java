#!/bin/bash

OUT=out/production/itmo-java
ARTIFACTS=java-advanced-2020/artifacts
LIBS=java-advanced-2020/lib
SOL=java-advanced-2020-solutions/java-solutions/ru/ifmo/rain/romanenko/$1
MODULE_TEST=info.kgeorgiy.java.advanced.$3
MODULE_SOL=ru.ifmo.rain.romanenko.$1.$4
CLASS=$2
SALT=$5

javac -cp "$ARTIFACTS/*" -d "$OUT" -p "$ARTIFACTS":"$LIBS" $SOL/*.java
java -cp "$ARTIFACTS/*":"$OUT" -p "$ARTIFACTS":"$LIBS" -m "$MODULE_TEST" "$CLASS" "$MODULE_SOL" "$SALT"