#!/bin/bash

mkdir db
rm db/database.db
sqlite3 db/database.db << EOF
create table data(id integer primary key autoincrement, text longtext, type char(5), policy char(20), expires_at timestamp, max_visits int, visits int);
EOF

