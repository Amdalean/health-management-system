echo 一键覆盖本地仓，使用前请做好备份！
cd ..
git fetch --all
git reset --hard origin/master
git pull