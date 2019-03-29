#!/bin/bash

# clone and enter directory
git clone https://github.com/TUDa-BP-11/cgm-remote-monitor && cd cgm-remote-monitor
git checkout -b uam
git branch --set-upstream-to=origin/uam uam
git pull

npm install

# start server in background
npm run start > server.log 2>&1 &

# go back to original directory
cd ..
# insert default profile
mongo --eval "db.profile.insert(`cat profile.json`)" nightscout
