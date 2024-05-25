#!/bin/bash
# run autotests in current maven parent folder and copy all test autoput to backup

# initialisation
pre_modules="tsl2.nano."
ref_time=$(date "+%Y-%m-%d-%H_%M_%S")
DIR_BACKUP=${1:-../../backup/autotest/target-$ref_time}
LOG_FILE=$DIR_BACKUP/$0.log
LOG_REF_TIME=$DIR_BACKUP/../ref_time.log

mkdir -p $DIR_BACKUP
echo "run on $ref_time" > $LOG_FILE

# maven-run
mvn -fn clean install -Dtest=AllAutoTests | tee $LOG_FILE

# backup
cp -rp target $DIR_BACKUP
cp -rp --parents **/target/autotest/generated $DIR_BACKUP

# analysis
if [ -f $LOG_FILE ]; then
    shopt -s globstar
    grep -r TESTED **/generated-autotests*statistics.txt > $DIR_BACKUP/../autotests-all-$ref_time.txt
    diff-ignore.sh /$DIR_BACKUP/../autotests-all-$(tail -n1 $LOG_REF_TIME).txt $DIR_BACKUP/../autotests-all-$ref_time.txt | tee -a $LOG_FILE
fi
echo -e "autotest reference-time:\n$ref_time" >> $LOG_FILE >> $LOG_REF_TIME

less -G $LOG_FILE
