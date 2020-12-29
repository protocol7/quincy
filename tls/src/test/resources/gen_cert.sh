#!/bin/bash

openssl genrsa -out server.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform DER -in server.pem  -nocrypt > server.der
openssl req -new -nodes -key server.pem -out csr.pem -subj /CN=quincy
openssl req -x509 -nodes -sha256 -days 36500 -key server.pem -in csr.pem -out server.crt

rm server.pem
rm csr.pem
