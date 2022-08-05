#!/bin/sh
echo "执行: pullDiffClass.sh"
contrastBranch=$1
currentBranch=$2
classDir=$3
contrastDir=$4
currentDir=$5

echo "start checkout: git checkout $contrastBranch"
git checkout $contrastBranch
git pull
git diff $contrastBranch $currentBranch --name-only --relative $classDir | xargs -n1 -I {} rsync -R {} $contrastDir"$(dirname {})"

echo "start checkout: git checkout $currentBranch"
git checkout $currentBranch
git pull
git diff $contrastBranch $currentBranch --name-only --relative $classDir | xargs -n1 -I {} rsync -R {} $currentDir"$(dirname {})"

echo "copy over --"