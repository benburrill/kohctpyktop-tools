import base64 as b64
import zlib
import textwrap
import string
from collections import namedtuple


def decode_save(save):
    return zlib.decompress(b64.b64decode(save))

def encode_save(data):
    return '\n'.join(textwrap.wrap(
        b64.b64encode(zlib.compress(data, level=9)).decode(),
        width=64
    ))

GridLayers = namedtuple('GridLayers', [
    'metadata', 'silicon', 'metal', 'vgates', 'hgates', 'vias', 
    'hscon', 'vscon', 'hmcon', 'vmcon'
])

CSEP = b'\t7\x01'
LSEP = b'\tY\x01' + CSEP

def stratify(data):
    layers = data.split(LSEP)
    return GridLayers(*[
        [bytearray(col) for col in layer.split(CSEP)]
        for layer in layers
    ])

def homogenize(layers):
    return LSEP.join(CSEP.join(columns) for columns in layers)

def show_encoded(layers):
    print(encode_save(homogenize(layers)))

def show_layers(layers):
    for name, layer in zip(GridLayers._fields, layers):
        print(f'----- [ LAYER {name} ] -----')

        for c, column in enumerate(layer):
            print(f'{c:<2}', end=' | ')
            print(' '.join(f'{byte:02x}' for byte in column))


if __name__ == "__main__":
    import sys

    if len(sys.argv) == 1:
        sys.stderr.write("Reading from stdin, ctrl-d to end input.\n")
        sys.stderr.flush()
        fd = sys.stdin
    elif len(sys.argv) == 2:
        fd = open(sys.argv[1])
    else:
        raise TypeError("Too many arguments")

    with fd:
        data = decode_save(fd.read())
        show_layers(stratify(data))
