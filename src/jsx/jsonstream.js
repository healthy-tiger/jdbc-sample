/**
 * @generator
 * @yields {Object}
 * @async
 * @param {ReadableStreamDefaultReader} source 
 */
async function* read(source) {
    /** @type {Array<ArrayBufferLike>} */
    const buffer = [];
    const sizebuffered = () => {
        let total = 0;
        buffer.forEach(e => {
            total += e.byteLength;
        });
        return total;
    };
    const slice = n => {
        const b = new Uint8Array(n);
        let remain = n;
        while(remain > 0) {
            const tmpbuf = buffer.shift();
            const tmparr = new Uint8Array(tmpbuf).subarray(0, Math.min(remain, tmpbuf.byteLength));
            if(tmpbuf.byteLength > remain) {
                buffer.unshift(tmpbuf.slice(remain));
            }
            b.set(tmparr, n - remain);
            remain -= tmparr.byteLength;
        }
        return b.buffer;
    };
    const getnext = async n => {
        while(sizebuffered() < n) {
            const { value, done } = await source.read();
            if(done) {
                return null;
            }
            buffer.push(value);
        }
        return slice(n);
    }
    const decoder = new TextDecoder();
    while(true) {
        const lenbytes = await getnext(Uint32Array.BYTES_PER_ELEMENT);
        if(lenbytes == null) {
            break;
        }
        const len = new DataView(lenbytes).getUint32(0, false);
        const bodybytes = await getnext(len);
        if(bodybytes == null) {
            throw new Error('The stream was closed unexpectedly.');
        }
        const s = decoder.decode(bodybytes);
        yield JSON.parse(s);
    }
}

export default read;

