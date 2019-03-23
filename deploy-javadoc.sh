#!/bin/bash

ant generate-javadoc

cd doc

git init
git remote add javadoc https://github.com/TUDa-BP-11/opendiabetes-uam-heuristik.git
git fetch --depth=1 javadoc gh-pages
git add --all
git commit -m "javadoc"
git merge --no-edit --allow-unrelated-histories -s ours remotes/javadoc/gh-pages
git push javadoc master:gh-pages

rm -rf .git
