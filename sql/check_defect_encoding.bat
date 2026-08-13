@echo off
chcp 65001
sqlplus -S prb/rmn_ppb@//172.16.21.45:1521/plusora @"sql\check_defect_encoding.sql"