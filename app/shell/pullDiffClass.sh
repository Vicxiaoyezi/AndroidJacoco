#!/bin/sh
echo "执行 pullDiffClass.sh"
echo "执行命令：git symbolic-ref --short HEAD"
currentBranch=$(git symbolic-ref --short HEAD)
echo "当前分支：$currentBranch"

branchName=$1
workDir=$2
outDir=$3
echo "branchName=$branchName"
echo "workDir=$workDir"
echo "outDir=$outDir"

echo "start checkout: git checkout $branchName"
git checkout $branchName

echo "start pull--"
git pull

echo "start copy: cp -r "${workDir}/app/classes" $outDir "
cp -r "${workDir}/app/classes" $outDir

echo "copy over --"