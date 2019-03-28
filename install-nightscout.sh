#!/bin/bash

git clone https://github.com/TUDa-BP-11/cgm-remote-monitor && cd cgm-remote-monitor
git checkout -b uam
git pull origin uam

npm install

# start server in subshell
(npm run start)
