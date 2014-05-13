# Muon

Muon is a self-destructible private file host. You run this on your web server and upload files
and text for your friends. You can set a limit to number of views or a time limit after which a
file will self-destruct and won't be available anymore.

It's really simple, I made this in an afternoon for myself. If you like it, feel free to use it.

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.
You will also need an sqlite3 installation available on your machine.

[1]: https://github.com/technomancy/leiningen

## Setup

Run:

    ./create-db.sh

to create a new sqlite3 database and set up the proper table/schema.

Modify the file src/muon/handler.clj to fit your domain and secret
password.

## Running

To start a web server for the application, run:

    lein ring server 8080

This starts the web server on port 8080, use whatever
other port you prefer for your setup/routing.

If you want to keep a repl connection alive for live monitoring of your webserver, run the following
commands:

    lein repl
    (use '[muon.handler])
    (use '[ring.util.serve])
    (serve app 8080)

It's better if you run it inside a screen or tmux session so you can detach whenever you want.
Again, change the port if necessary.

## License

Copyright © 2014 Morgawr
