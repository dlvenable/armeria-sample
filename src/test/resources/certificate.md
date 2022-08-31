
Generated via:

```
openssl req -x509 -newkey rsa:2048 -keyout src/test/resources/key.pem -out src/test/resources/cert.pem -sha256 -days 365 -nodes 
```


Generates an alternate certificate. This is used to verify the behavior of an unknown certificate.

```
openssl req -x509 -newkey rsa:2048 -keyout src/test/resources/alt-key.pem -out src/test/resources/alt-cert.pem -sha256 -days 365 -nodes
```

