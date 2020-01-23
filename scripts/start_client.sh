#!/bin/bash

if [ $# -eq 0 ]; then
	echo "Usage: $0 input_file [zoo_port] [debug]"
	exit -1
fi

set =x

file=$1
port=$2
debug=$3

[[ -z $port ]] && port=2181

java -jar Client.jar $file $port $debug
