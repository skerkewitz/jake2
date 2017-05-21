#!/bin/sh

source="https://ftp.gwdg.de/pub/misc/ftp.idsoftware.com/idstuff/quake2/q2-314-demo-x86.exe"

mkdir /tmp/q2
cd /tmp/q2
curl $source -o quake2.zip
unzip quake2.zip
cp -r ./Install/Data/baseq2/ ~/q2

