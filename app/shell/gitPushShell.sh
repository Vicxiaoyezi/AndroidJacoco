#!/bin/sh
echo "执行: gitPushSell.sh"
currentBranch=$(git symbolic-ref --short HEAD)
git pull
git add -f $1
git commit -m "$2"
git push origin $currentBranch
echo "end push: --"