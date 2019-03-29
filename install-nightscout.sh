#!/bin/bash

git clone https://github.com/TUDa-BP-11/cgm-remote-monitor && cd cgm-remote-monitor
git checkout -b uam
git branch --set-upstream-to=origin/uam uam
git pull

npm install

# start server in subshell
(npm run start)
