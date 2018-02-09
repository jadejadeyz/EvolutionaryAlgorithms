#!/bin/bash

dir=$(dirname "$0")
sh_scipt="/runTSP_734.sh"
pycode="/main.py"
args_file="/args_setting_734.tsv"
data_file="/A2_TSP_Uruguay_734.txt"

chmod +x $dir$sh_script
python $dir$pycode $dir$args_file $dir$data_file 1
