#!/usr/bin/env python3
"""Check 64-bit ELF load alignment and RELRO page overlap in an APK.

Static validation only; this does not replace testing on a 16 KB device.
"""
import sys,zipfile,struct,json
from pathlib import Path
if len(sys.argv)!=2:
 raise SystemExit('Usage: python3 scripts/check-apk-alignment.py path/to/app.apk')
apk=Path(sys.argv[1]);results=[]
with zipfile.ZipFile(apk) as z:
 for name in z.namelist():
  if not name.endswith('.so') or not any(abi in name for abi in ['/arm64-v8a/','/x86_64/']):continue
  b=z.read(name);is64=b[4]==2;e='<' if b[5]==1 else '>'
  off=struct.unpack_from(e+('Q' if is64 else 'I'),b,32 if is64 else 28)[0]
  size,count=struct.unpack_from(e+'HH',b,54 if is64 else 42)
  loads=[];relros=[]
  for i in range(count):
   if is64:t,fl,offset,virt,phys,fs,ms,align=struct.unpack_from(e+'IIQQQQQQ',b,off+i*size)
   else:t,offset,virt,phys,fs,ms,fl,align=struct.unpack_from(e+'IIIIIIII',b,off+i*size)
   if t==1:
    assert align>=16384,(name,align)
    assert offset%16384==virt%16384
    loads.append((virt,virt+ms,fl,align))
   if t==0x6474e552:relros.append((virt,virt+ms))
  # Protecting a rounded RELRO page must not cover unrelated writable data.
  for start,end in relros:
   lo=start//16384*16384;hi=(end+16383)//16384*16384
   for a,b,fl,_ in loads:
    if not fl&2:continue
    covered_a=max(a,lo);covered_b=min(b,hi)
    assert covered_a>=covered_b or (covered_a>=start and covered_b<=end),(name,'RELRO overlaps writable data')
  results.append({'library':name,'load_alignment':min(l[3] for l in loads),'relro_separate_from_other_writable_data':True})
 print(json.dumps({'apk':str(apk),'native_libraries':results},indent=2))
