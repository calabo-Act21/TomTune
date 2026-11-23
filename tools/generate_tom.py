#!/usr/bin/env python3
"""Generate self-contained glTF 2 GLBs for the feasibility scene, without external tools.
World: x right, y up, z towards viewer. Canonical lug x/y -> world x/z.
Run from repository root; output is deterministic and checked in.
"""
import json
import math
from pathlib import Path
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]


def cylinder(radius, bottom, top, segments=64):
    p, n, ix = [], [], []
    for i in range(segments):
        a, b = i * math.tau / segments, (i + 1) * math.tau / segments
        start = len(p)
        for angle, y in [(a, bottom), (a, top), (b, top), (b, bottom)]:
            p.append((radius*math.cos(angle), y, radius*math.sin(angle)))
            n.append((math.cos(angle), 0, math.sin(angle)))
        ix.extend(start+k for k in [0, 1, 2, 0, 2, 3])
        for y, normal, order in [(top, (0, 1, 0), [0, 2, 1]), (bottom, (0, -1, 0), [0, 1, 2])]:
            start = len(p)
            p.extend([(0, y, 0), (radius*math.cos(a), y, radius*math.sin(a)), (radius*math.cos(b), y, radius*math.sin(b))])
            n.extend([normal]*3)
            ix.extend(start+k for k in order)
    return p, n, ix


def torus(major, minor, y, segments=80, sides=8):
    p, n, ix = [], [], []
    for i in range(segments+1):
        a = i*math.tau/segments
        for j in range(sides+1):
            b = j*math.tau/sides
            p.append(((major+minor*math.cos(b))*math.cos(a), y+minor*math.sin(b), (major+minor*math.cos(b))*math.sin(a)))
            n.append((math.cos(b)*math.cos(a), math.sin(b), math.cos(b)*math.sin(a)))
    for i in range(segments):
        for j in range(sides):
            k = i*(sides+1)+j
            ix.extend([k, k+1, k+sides+2, k, k+sides+2, k+sides+1])
    return p, n, ix


FONT = {
'T':['11111','00100','00100','00100','00100','00100','00100'],
'o':['00000','00000','01110','10001','10001','10001','01110'],
'm':['00000','00000','11010','10101','10101','10101','10101'],
'u':['00000','00000','10001','10001','10001','10011','01101'],
'n':['00000','00000','11110','10001','10001','10001','10001'],
'e':['00000','00000','01110','10001','11111','10000','01110']}


def logo():
    p, n, ix = [], [], []
    pixel = .012
    width = (len('TomTune')*6-1)*pixel
    for c, letter in enumerate('TomTune'):
        for row, cells in enumerate(FONT[letter]):
            for col, bit in enumerate(cells):
                if bit != '1':
                    continue
                x, z = -width/2+(c*6+col)*pixel, -.74+row*pixel
                start = len(p)
                p.extend([(x,.034,z), (x,.034,z+pixel*.87), (x+pixel*.87,.034,z+pixel*.87), (x+pixel*.87,.034,z)])
                n.extend([(0,1,0)]*4)
                ix.extend(start+k for k in [0,1,2,0,2,3])
    return p,n,ix


def build(count):
    binary = bytearray()
    doc = {'asset':{'version':'2.0','generator':'TomTune procedural feasibility model'},'scene':0,'scenes':[{'nodes':[]}],
           'nodes':[], 'meshes':[], 'bufferViews':[], 'accessors':[], 'materials':[]}
    def material(name, color, metal=0, rough=.5, emissive=None):
        mat = {'name':name, 'pbrMetallicRoughness':{'baseColorFactor':color+[1], 'metallicFactor':metal,'roughnessFactor':rough}}
        if emissive: mat['emissiveFactor'] = emissive
        doc['materials'].append(mat)
        return len(doc['materials'])-1
    wood = material('walnut', [.25,.115,.045], rough=.4)
    skin = material('ivory membrane', [.82,.79,.69], rough=.85)
    steel = material('brushed steel', [.65,.67,.68], .82,.24)
    ink = material('logo ink', [.02,.02,.02], rough=1)
    blue = material('selection', [.015,.2,1], .15,.26,[.015,.12,.8])
    ground = material('warm background', [.69,.66,.61],rough=1)
    def accessor(values, kind, component=5126):
        while len(binary)%4: binary.append(0)
        start = len(binary)
        if kind == 'SCALAR':
            binary.extend(struct.pack('<'+'H'*len(values),*values))
        else:
            binary.extend(struct.pack('<'+'f'*(len(values)*3),*(v for row in values for v in row)))
        doc['bufferViews'].append({'buffer':0,'byteOffset':start,'byteLength':len(binary)-start})
        a = {'bufferView':len(doc['bufferViews'])-1,'componentType':component,'count':len(values),'type':kind}
        if kind == 'VEC3':
            a['min'] = [min(v[i] for v in values) for i in range(3)]
            a['max'] = [max(v[i] for v in values) for i in range(3)]
        doc['accessors'].append(a)
        return len(doc['accessors'])-1
    def mesh(geometry, mat):
        p,n,ix = geometry
        doc['meshes'].append({'primitives':[{'attributes':{'POSITION':accessor(p,'VEC3'),'NORMAL':accessor(n,'VEC3')},'indices':accessor(ix,'SCALAR',5123),'material':mat}]})
        return len(doc['meshes'])-1
    def node(name, mesh_id, position=(0,0,0)):
        doc['scenes'][0]['nodes'].append(len(doc['nodes']))
        doc['nodes'].append({'name':name,'mesh':mesh_id,'translation':list(position)})
    node('shell',mesh(cylinder(.98,-.68,0),wood))
    node('head',mesh(cylinder(.97,.008,.025),skin))
    node('upper_hoop',mesh(torus(1,.027,.025),steel))
    node('lower_hoop',mesh(torus(1,.027,-.66),steel))
    node('TomTune',mesh(logo(),ink))
    node('floor',mesh(cylinder(2.4,-.77,-.75),ground))
    cap = mesh(cylinder(.055,.04,.12,12),steel)
    rod = mesh(cylinder(.018,-.57,.055,12),steel)
    halo = mesh(torus(.082,.013,.10,32,8),blue)
    for i in range(count):
        angle = -math.pi/2+math.tau*(i+.5)/count
        xyz = (1.035*math.cos(angle),0,1.035*math.sin(angle))
        node(f'lug_{i}',cap,xyz)
        node(f'rod_{i}',rod,xyz)
        node(f'selection_{i}',halo,xyz)
    # Fine alternating walnut staves provide a geometric wood cue without an image texture.
    for i in range(40):
        a = i*math.tau/40
        radius=.982
        p=[(radius*math.cos(a),-.64,radius*math.sin(a)),(radius*math.cos(a),-.02,radius*math.sin(a)),
           (radius*math.cos(a+.009),-.02,radius*math.sin(a+.009)),(radius*math.cos(a+.009),-.64,radius*math.sin(a+.009))]
        normal=(math.cos(a),0,math.sin(a))
        node(f'grain_{i}',mesh((p,[normal]*4,[0,1,2,0,2,3]),ink))
    doc['buffers']=[{'byteLength':len(binary)}]
    j=json.dumps(doc,separators=(',',':')).encode()
    j+=b' '*((-len(j))%4)
    binary+=b'\x00'*((-len(binary))%4)
    return struct.pack('<III',0x46546c67,2,12+8+len(j)+8+len(binary))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(binary),0x004e4942)+binary


def verify(data, count):
    """Check encoded buffers, index bounds, scene identities and canonical lug placement."""
    magic, version, size = struct.unpack_from('<III', data)
    assert (magic, version, size) == (0x46546c67, 2, len(data))
    length, kind = struct.unpack_from('<II', data, 12)
    assert kind == 0x4e4f534a
    doc = json.loads(data[20:20+length])
    at = 20+length
    blen, btype = struct.unpack_from('<II', data, at)
    binary = data[at+8:]
    assert btype == 0x004e4942 and blen == len(binary)
    for a in doc['accessors']:
        v = doc['bufferViews'][a['bufferView']]
        width = 3 if a['type'] == 'VEC3' else 1
        element_size = 4 if a['componentType'] == 5126 else 2
        assert v['byteOffset'] % 4 == 0
        assert v['byteLength'] == a['count'] * width * element_size
        assert v['byteOffset'] + v['byteLength'] <= doc['buffers'][0]['byteLength']
    for mesh in doc['meshes']:
        for primitive in mesh['primitives']:
            a = doc['accessors'][primitive['indices']]
            v = doc['bufferViews'][a['bufferView']]
            indices = struct.unpack_from('<'+'H'*a['count'], binary, v['byteOffset'])
            positions = doc['accessors'][primitive['attributes']['POSITION']]
            assert len(indices) % 3 == 0 and max(indices) < positions['count']
    assert len([n for n in doc['nodes'] if n['name'].startswith('lug_')]) == count
    for i in range(count):
        angle = -math.pi/2 + math.tau*(i+.5)/count
        lug = next(n for n in doc['nodes'] if n['name'] == f'lug_{i}')
        assert abs(lug['translation'][0]-1.035*math.cos(angle)) < 1e-12
        assert abs(lug['translation'][2]-1.035*math.sin(angle)) < 1e-12
        assert sum(n['name'] == f'selection_{i}' for n in doc['nodes']) == 1
    assert any(n['name'] == 'TomTune' for n in doc['nodes'])


if __name__ == '__main__':
    check = '--check' in sys.argv
    for count in [5, 6, 8, 10]:
        output = ROOT/f'shared/src/androidMain/assets/tom/tom-{count}.glb'
        expected = build(count)
        if check:
            actual = output.read_bytes()
            assert actual == expected, f'{output}: regenerate stale GLB'
            verify(actual, count)
            print(f'N={count}: valid deterministic GLB, buffers, triangles, lug identities and angles')
        else:
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(expected)
            print(f'{output.relative_to(ROOT)}: {output.stat().st_size} bytes')
