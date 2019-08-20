#/bin/bash
echo "unlinking old symlink"
touch resources/webapp
unlink resources/webapp
echo "linking new build"
ln -svf "$(pwd)/frontend/build/es6-bundled" "$(pwd)/resources/webapp"