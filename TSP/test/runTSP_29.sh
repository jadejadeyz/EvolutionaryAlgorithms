#!/bin/bash

dir=$(dirname "$0")
sh_scipt="/runTSP_29.sh"
pycode="/main.py"
args_file="/args_setting_29.tsv"
data_file="/A2_TSP_WesternSahara_29.txt"

chmod +x $dir$sh_script
python $dir$pycode $dir$args_file $dir$data_file 1
