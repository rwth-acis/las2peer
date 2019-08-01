#/bin/bash
echo "unlinking old symlink"
touch /app/las2peer/webconnector/resources/webapp
unlink /app/las2peer/webconnector/resources/webapp
echo "linking new build"
ln -svf /app/las2peer/webconnector/frontend/build/es6-bundled /app/las2peer/webconnector/resources/webapp