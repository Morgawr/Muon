#!/bin/bash

mkdir db 2> /dev/null
rm db/database.db 2> /dev/null
sqlite3 db/database.db << EOF
create table data(id integer primary key autoincrement, text longtext, type char(5), policy char(20), expires_at timestamp, max_visits int, visits int);
EOF

