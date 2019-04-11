#!/usr/bin/env bash

mkdir db 2> /dev/null
rm db/database.db 2> /dev/null
sqlite3 db/database.db << EOF
create table data(id integer primary key autoincrement, folder text, filename text, text text, type text, policy text, expires_at timestamp, max_visits int, visits int);
create table autoexpire(id integer primary key autoincrement, data_id integer, expires_at timestamp);
EOF
