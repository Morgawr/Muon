#!/usr/bin/env bash

function gen_dict() {
	DICT=$1

}
DICTFILE=/usr/share/dict/words
OUTFILE=db/wordlist
if test -f "$DICTFILE"; then
	# Get all words longer than 5 characters and save them in their own wordlist file
	awk 'length>5' $DICTFILE | LC_CTYPE=C awk '! /[^[:alnum:]]/' | uniq > $OUTFILE
else
	(>&2 echo "Error: The file ${DICTFILE} does not exist. Cannot generate wordlist for file indexing.")
	exit 1
fi

