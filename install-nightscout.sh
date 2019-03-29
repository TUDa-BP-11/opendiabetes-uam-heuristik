#!/bin/bash

git clone https://github.com/TUDa-BP-11/cgm-remote-monitor && cd cgm-remote-monitor
git checkout -b uam
git branch --set-upstream-to=origin/uam uam
git pull

npm install

# start server in background
npm run start > server.log 2>&1 &

# insert default profile
mongo --eval "db.profile.insert(`cat profile.json`)" nightscout
