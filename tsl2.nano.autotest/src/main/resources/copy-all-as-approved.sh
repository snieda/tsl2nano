#!/bin/bash
# copy all generated autotests to the src/test/resources folders of the modules of a multimodule project
. mainargs.sh "$@"
TST_BASE=${1:-./}
PRJ_BASE=${PRJ_BASE:-~/workspace/tsl2nano-code}
PRJ_MODULE_PREFIX=${PRJ_MODULE_PREFIX:-"tsl2.nano"}

cd $TST_BASE
for f in $(ls) ; do \
    [[ $f == $PRJ_MODULE_PREFIX* ]] \
    && SRC_PATH="$PRJ_BASE/$f/src/test/resources" \
    && mkdir -p $SRC_PATH \
    && cp -rv $f/target/* $SRC_PATH \
    && mv $SRC_PATH/target/* $SRC_PATH \
    && rm $SRC_PATH/target; \
    done