#!/usr/bin/env python3
"""Genera una CA propia y certificado DNS en directorio nuevo; jamás sobreescribe."""
import argparse
import os
from pathlib import Path
import re
import subprocess
p=argparse.ArgumentParser();p.add_argument('hostname');p.add_argument('output',type=Path);a=p.parse_args()
if not re.fullmatch(r'[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?',a.hostname) or '..' in a.hostname: raise SystemExit('Hostname inválido')
a.output.mkdir(parents=True,exist_ok=False); os.chmod(a.output,0o700)
server=a.output/'server';server.mkdir(); ca=a.output/'ca-private';ca.mkdir(mode=0o700)
def run(*args):subprocess.run(['openssl',*map(str,args)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
run('req','-x509','-newkey','rsa:3072','-nodes','-days','365','-subj','/CN=ExamenErik demo CA','-keyout',ca/'ca-key.pem','-out',server/'ca.pem')
run('req','-new','-newkey','rsa:3072','-nodes','-subj','/CN='+a.hostname,'-keyout',server/'server-key.pem','-out',ca/'server.csr')
ext=ca/'extensions.cnf';ext.write_text('subjectAltName=DNS:'+a.hostname+'\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n')
run('x509','-req','-in',ca/'server.csr','-CA',server/'ca.pem','-CAkey',ca/'ca-key.pem','-CAcreateserial','-days','90','-extfile',ext,'-out',server/'server-cert.pem')
for directory in [server,ca]:
 for file in directory.iterdir():file.chmod(0o600 if 'key' in file.name else 0o644)
print('Generado. CA privada queda fuera del montaje; certificado servidor vence en 90 días. Ajusta propietario de server-key.pem al UID mysql de la imagen.')
