# Kohctpyktop tools

Switching to Java, below is for Python...

---

Currently pretty minimal

What I've found out about the format I've written in the file `format`.
The file `kohsave.py` has a few functions for working with saves.

Example usage

```pycon
>>> from kohsave import *
>>> save = stratify(decode_save(open('saves/empty.sav').read()))
>>> 
>>> # draw a 20-length vertical line at column 5
>>> for i in range(20):
...     save.metal[5][i] = 3
...     save.vmcon[5][i] = 3
...
>>> show_encoded(save)
eNrt2j0KgCAABtBKl87QFdo7S/e/SNDUYGJF2M9TnB4fOn0IGsc49HPbT21szkxB
QUFBQUFBQcH6wfU61+2PVUMIyXVdc/uGxNieKn9mSulXtUYjFVUlpZT+W1UlpZSq
SkopVZWUUqoqKaX0NVV51xs3pZQe1xqNVFiV6XVdc/v6LEQpfUgjLeSSxks=
```

You can load this save, which will have the vertical line.

Our real goal is not to create saves though, it's to take a save and
convert it into a graph and then ideally do stuff with the graph.
