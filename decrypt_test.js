
    const CryptoJS = require('crypto-js');
    function oyunculistdc(passphrase,encrypted_json_string){ 
        var obj_json = JSON.parse(encrypted_json_string); 
        var encrypted = obj_json.ciphertext; 
        var salt = CryptoJS.enc.Hex.parse(obj_json.salt); 
        var iv = CryptoJS.enc.Hex.parse(obj_json.iv); 
        var key = CryptoJS.PBKDF2(passphrase, salt, { hasher: CryptoJS.algo.SHA512, keySize: 64/8, iterations: 999}); 
        var decrypted = CryptoJS.AES.decrypt(encrypted, key, { iv: iv}); 
        return decrypted.toString(CryptoJS.enc.Utf8); 
    }
    console.log(oyunculistdc('3hPn4uCjTVtfYWcjIcoJQ4cL1WWk1qxXI39egLYOmNv6IblA7eKJz68uU3eLzux1biZLCms0quEjTYniGv5z1JcKbNIsDQFSeIZOBZJz4is6pD7UyWDggWW', "{\"ciphertext\":\"UtAEAuYTBKaawbG243JhGe0W4JFnWr7kMwdO2skrnufSpHb1iA4krmqxNIzmJeUm+sTV\\/qZBxgQy1VGQ+JRirEqPskF8zi2Fo2vwVev+iKQ=\",\"iv\":\"d7b5eab8d6629f1aba41d06c8b6c4be4\",\"salt\":\"2848c4252f18feb19c44a9e894b338c1d102db7b7f90e66ca8173595b9bd4e872698d4b23af625f436726afea311f24544c4eb1de40ed6e59e91afa4229ded1d499d2feb9695deebef4f46884d872af271d000c6cd50f0eee13ca131896767d3f529537159af75177259556179681bb0b799bb51c4534fddb8d7d4abdb9b11c8189ef278502956a29da97e1d1c20d896966915083a0926a33213cb17dd22b61827c8b56c3c08e39941bfef1f202d2f266a1364019ac794859441a19e7c5feaae16de6a895d3c4749d5042160f37bb6fae247b255920552d8b8722ab26c3401a479a80f47f69b836ffd90a0a98261d593dda0f7356ea75ec1c0527e6f1a533947\"}"));
    