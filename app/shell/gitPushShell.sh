#!/bin/sh
echo "执行 gitPushSell.sh"
echo "执行命令：git symbolic-ref --short HEAD"
currentBranch=$(git symbolic-ref --short HEAD)
echo "git push 当前分支：$currentBranch"

echo "start checkout: git checkout $currentBranch"
git checkout $currentBranch

git pull

echo "start: git add -f app/classes/--"
git add -f app/classes/

echo "start commit: git commit -m "$1""
git commit -m "$1"

echo "start push: git push origin $currentBranch"
git push origin $currentBranch

echo "end push --"