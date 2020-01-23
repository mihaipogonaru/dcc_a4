#!/bin/bash

if [[ ! $# -eq 5 ]]; then
	echo "Usage $0 folder zoo_ip zoo_port memcached_ip memcached_port"
	exit -1
fi

set -x

folder=$1
zoo_ip=$2
zoo_port=$3
memcached_ip=$4
memcached_port=$5

mkdir -p $folder

cp Watcher.jar $folder
cp scripts/start_watcher.sh $folder

cd $folder
echo -e "${zoo_ip} ${zoo_port}\n${memcached_ip} ${memcached_port}" &> watcher.cfg
