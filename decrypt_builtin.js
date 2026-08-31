const crypto = require('crypto');
const fs = require('fs');

function decrypt(passphrase, rawJsonStr) {
    const obj = JSON.parse(rawJsonStr);
    const salt = Buffer.from(obj.salt, 'hex');
    const iv = Buffer.from(obj.iv, 'hex');
    const ciphertext = Buffer.from(obj.ciphertext, 'base64');
    
    // CryptoJS keySize: 64/8 is 8 words = 32 bytes (256-bit key)
    const key = crypto.pbkdf2Sync(passphrase, salt, 999, 32, 'sha512');
    const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
    let decrypted = decipher.update(ciphertext);
    decrypted = Buffer.concat([decrypted, decipher.final()]);
    return decrypted.toString('utf8');
}

const raw = JSON.parse(fs.readFileSync('payload.json', 'utf8'));
const key = '3hPn4uCjTVtfYWcjIcoJQ4cL1WWk1qxXI39egLYOmNv6IblA7eKJz68uU3eLzux1biZLCms0quEjTYniGv5z1JcKbNIsDQFSeIZOBZJz4is6pD7UyWDggWW';
console.log('Decrypted URL:', decrypt(key, JSON.stringify(raw)));
