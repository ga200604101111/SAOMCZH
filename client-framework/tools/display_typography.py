"""Normalize Chinese output-field separators without changing matcher keys or numbers."""
import re
FORMAT=re.compile(r'(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])')
SPACES=' \t\u00a0\u3000'
def normalize_fields(raw, aliases=()):
 if not isinstance(raw,str) or (':' not in raw and '：' not in raw):return raw
 text=[];start=[];end=[];i=0
 while i<len(raw):
  match=FORMAT.match(raw,i)
  if match:i=match.end();continue
  start.append(i);text.append(raw[i]);i+=1;end.append(i)
 text=''.join(text);out=raw
 for at in range(len(text)-1,-1,-1):
  if text[at] not in ':：':continue
  left=at-1;right=at+1
  while left>=0 and text[left] in SPACES:left-=1
  while right<len(text) and text[right] in SPACES:right+=1
  if left<0:continue
  cp=ord(text[left])
  if not (0x3400<=cp<=0x9fff or 0x20000<=cp<=0x323af or cp in aliases):continue
  a=end[left];b=start[right] if right<len(text) else len(raw);part=raw[a:b];fixed=[];i=0
  while i<len(part):
   match=FORMAT.match(part,i)
   if match:fixed.append(match.group());i=match.end();continue
   ch=part[i];i+=1
   if ch in SPACES:continue
   fixed.append(':' if ch=='：' else ch)
  changed=''.join(fixed)+' '
  if changed!=part:out=out[:a]+changed+out[b:]
 return out
