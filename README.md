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

The webserver is designed to run from a repl. This makes it easier to run live queries and do
live debugging and... it's cool. It's recommended to start the repl in a tmux or screen session.

To start, run the following command in the home directory of the project:

    lein repl

And then execute the following statements in the repl, in order:

    (use '[muon.handler])
    (use '[ring.util.serve])
    (serve app 8080)

Again, change the port if necessary.

## API

The API is really simple. You can either upload a file or some text, any file goes.
Take a look at muon-up, the primitive example client written in bash, to get a feel of the API.

https://github.com/Morgawr/muon-up

## License

The MIT License (MIT)

Copyright (c) 2014 Federico "Morg" Pareschi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

