# This is a python implementation of the satochip_sign_message command


@main.command()
@click.option("--keyslot", default="255", help="keyslot of the private key (for single-key wallet")
@click.option("--path", default="m/44'/0'/0'/0/0", help="path: the full BIP32 path of the address")
@click.option("--message", required=True, help="The message to sign")
def satochip_sign_message(message, keyslot, path):
    """Sign a Message with the Satochip"""
    message_byte = message.encode('utf8')
    separator = ':'

    print(f"Sign a Message with the Satochip: message_byte: {message_byte}")

    try:
        # get PIN from environment variable or interactively
        if 'PYSATOCHIP_PIN' in environ:
            pin= environ.get('PYSATOCHIP_PIN')
            print("INFO: PIN value recovered from environment variable 'PYSATOCHIP_PIN'")
        else:
            pin = getpass("Enter your PIN:")

        print("card_verify_PIN for PIN:", pin)

        cc.card_verify_PIN(pin)
        # check if 2FA is required
        hmac=b''
        if (cc.needs_2FA==None):
            (response, sw1, sw2, d) = cc.card_get_status()
        if cc.needs_2FA:
            # challenge based on sha256(btcheader+msg)
            # format & encrypt msg
            msg = {'action':"sign_msg", 'msg':message}
            msg = json.dumps(msg)
            # do challenge-response with 2FA device...
            hmac = do_challenge_response(msg)
            hmac = bytes.fromhex(hmac)
        # derive key
        keyslot= int(keyslot)
        if keyslot == 0xFF:
            # 0xFF is for extended key, used if no keyslot is provided
            (depth, bytepath)= cc.parser.bip32path2bytes(path)
            print("card_bip32_get_extendedkey for path:", path)
            (pubkey, chaincode)= cc.card_bip32_get_extendedkey(bytepath)
        else:
            pubkey = cc.satochip_get_pubkey_from_keyslot(keyslot)
        # sign message
        print(f"PreSign: keyslot: {keyslot}, pubkey: {pubkey}, message_byte: {message_byte}, hmac: {hmac}")
        (response2, sw1, sw2, compsig) = cc.card_sign_message(keyslot, pubkey, message_byte, hmac)
        if  compsig == b'':
            print("Wrong signature: the 2FA device may have rejected the action.")
        else:
            print("Signature (Base64):", base64.b64encode(compsig).decode())
            print("Message length:", len(message_byte), "bytes")
            print("Seperator length:", len(separator), "bytes")
            print("Signature length:", len(compsig), "bytes")
            print(message_byte,separator,compsig.hex(), (len(message_byte) + len(separator) + len(compsig)), "bytes" )

    except Exception as e:
        print(e)

# OTHER FUNCTIONS CALLED BY satochip_sign_message
cc.card_get_status()
cc.card_verify_PIN(pin)
cc.card_get_status()
cc.card_sign_message(keyslot, pubkey, message_byte, hmac)

# This is an example of how to use the satochip_sign_message command

python3 satochip_cli.py satochip-sign-message --path "m/84'/0'/0'/0/0" --message 'hello, this is a signature test.'

# And the resultant output should be:

.venvZilla:pysatochip i830671$ python3 satochip_cli.py satochip-sign-message --path "m/84'/0'/0'/0/0" --message 'hello, this is a signature test.'
DEBUG: CardConnection.T0_protocol = 1
DEBUG: CardConnection.T1_protocol = 2
DEBUG: CardConnection.RAW_protocol = 65536
DEBUG: Trying T0 protocol...
DEBUG: Failed to connect using T0 protocol: Unable to connect with protocol: T0. Card protocol mismatch.: Card protocol mismatch. (0x8010000F)
DEBUG: Trying T1 protocol...
DEBUG: Successfully connected using T1 protocol
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: 00 a4 04 00 08 53 61 74 6f 43 68 69 70
NO ENCRYPTION: SELECT SATOCHIP
ENCRYPTED: APDU: 00 a4 04 00 08 53 61 74 6f 43 68 69 70
APDU: Response: , SW1: 90, SW2: 00
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 3c 00 00
NO ENCRYPTION: GET_STATUS
ENCRYPTED: APDU: b0 3c 00 00
APDU: Response: 00 0c 00 05 05 01 01 01 00 01 01 01, SW1: 90, SW2: 00
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 81 00 00 41 04 7b a2 4e fd f4 1f 8c 46 09 6d 72 92 08 f1 26 31 22 05 29 d4 c0 6c 9f 2f 71 4e e1 f1 b3 80 db f7 0f 44 09 6c d0 e3 d2 1e e3 13 e7 3f 47 aa e2 b5 5c 33 be 3a a9 3e a1 57 07 ba 11 bc 94 b3 25 ae
NO ENCRYPTION: INIT_SECURE_CHANNEL
ENCRYPTED: APDU: b0 81 00 00 41 04 7b a2 4e fd f4 1f 8c 46 09 6d 72 92 08 f1 26 31 22 05 29 d4 c0 6c 9f 2f 71 4e e1 f1 b3 80 db f7 0f 44 09 6c d0 e3 d2 1e e3 13 e7 3f 47 aa e2 b5 5c 33 be 3a a9 3e a1 57 07 ba 11 bc 94 b3 25 ae
APDU: Response: 00 20 f2 52 fc f3 54 46 74 d6 3b 89 36 73 43 ae ed 8b 82 f5 ae d5 4a 64 8a bf 97 96 72 7d 2f 82 04 d5 00 47 30 45 02 21 00 83 d8 48 f9 9b 8e 5f c0 7a 65 b7 32 43 40 97 5e dc 58 d2 66 8b 2c 72 cd 59 b7 a6 bf 59 14 6d 42 02 20 20 97 24 83 0b b7 c2 5a 7e b3 e0 d4 14 df e4 a4 ea bb 4a a6 4a f4 f1 f3 63 75 6b 06 1a aa 69 e8 00 47 30 45 02 21 00 9a 37 5f 0e 31 db a4 91 b1 c7 fe b1 a1 c8 aa af d1 8a 3d bb 5f e2 a6 a7 07 3e cc 1d a3 0b 46 fe 02 20 71 d5 f2 8d 8e 8b 89 b2 15 38 b6 0a db 5c 27 16 91 83 eb 42 b8 43 27 d7 c9 fd 2c 0b 60 de 1b bc, SW1: 90, SW2: 00
Sign a Message with the Satochip: message_byte: b'hello, this is a signature test.'
INFO: PIN value recovered from environment variable 'PYSATOCHIP_PIN'
card_verify_PIN for PIN: qqqq
card_bip32_get_extendedkey for path: m/84'/0'/0'/0/0
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 73 00 00
ENCRYPTION: BIP32_GET_AUTHENTIKEY
ENCRYPTED: APDU: b0 82 00 00 38 83 bb 31 5f b0 5f f7 fa 25 9a 2d 2e 00 00 00 03 00 10 85 73 35 16 a2 a5 f4 3b 93 ca dd ae 20 80 52 1a 00 14 25 90 50 b7 4b d6 e5 4a fc 98 a2 5f 4e 63 56 22 4a 7c a7 e0
APDU: Response: 73 eb 81 f6 7b 6e e7 03 d7 eb 11 92 00 00 00 04 00 70 f1 1d ba 76 00 81 41 71 bf 65 cc 7a 63 03 7c fa d2 31 9e a4 ad 44 79 31 64 52 e8 8b 4a 9e 8b a3 51 bf 0c e4 8f 9c d4 d9 b7 96 99 c0 73 ec cc f4 2b d6 99 e5 ad 07 83 99 57 1e 5e 27 59 0a 09 a8 bf 3b f4 10 8a 0e 3d 95 fc 16 52 48 6a 78 31 b9 9e 36 60 12 f5 7a 7f 4b 25 94 0a c5 9d 80 c5 7a 9b 5e c2 f5 09 99 d4 0e 36 14 8b be 5e fb 69 47, SW1: 90, SW2: 00
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 75 00 00 8e 00 20 c1 ea 37 d6 91 c0 3f 07 a8 3a 0d 71 b1 01 23 35 d0 36 df e0 39 64 bc 24 96 99 fb a7 26 26 f7 b7 00 48 30 46 02 21 00 8a 8c 2a 7c 1e fa cd 2e 84 3a cc 68 d7 3f 64 28 7d 1b 16 fd 7e 8b 81 b2 78 52 05 0c ea d6 f1 e9 02 21 00 ca 0f 92 53 2e 52 58 6f 58 32 b5 79 98 a0 1c 50 bd 90 b3 b9 44 42 0c 20 40 e3 be 02 eb d2 ab 03 00 20 9f 1a 76 f6 62 bf 55 d6 7f 10 2f 51 75 de 9b ca 75 3c ad 31 c9 d1 75 26 01 49 4c 48 70 b7 7b 32
ENCRYPTION: 75 BIP32_GET_AUTHENTIKEY_COORDX
ENCRYPTED: APDU: b0 82 00 00 c8 39 bb b4 cb 89 7f f5 69 08 58 87 92 00 00 00 05 00 a0 2d 3a 83 bc 5c 51 4c c1 2d fd b4 56 bf 8b a0 3d 26 67 c7 cd f7 fd 77 c2 69 0f 27 58 56 9e b1 81 ca e9 77 88 ff a0 c6 cb 69 af e3 88 c0 1c 12 80 ae 1a 30 f1 98 26 42 7c 9d c9 ae 02 56 43 88 39 e4 92 18 36 a5 54 cb ba 6d 5f c9 5a 5f a2 f0 35 cd 13 b3 45 91 b6 20 e3 b0 74 09 59 8e d1 5c 8e 21 c1 af 6b a9 f5 b0 07 b3 a6 6c 6b 91 b8 a7 19 8a c8 04 15 f3 0d 6c 8f aa 2c 6a 15 af 35 b4 bf 76 21 06 f7 f2 ae ca 0f b3 41 07 94 14 24 39 86 d6 3b 5d 95 5e 7e 94 74 b4 c4 bd 4c c3 2f 0f 44 00 14 e0 ce 07 92 e3 44 16 b7 46 5d ca ec 14 e7 79 7a 62 d3 a9 ab
APDU: Response: 61 6c db 3e f8 5b f6 8a 09 07 62 97 00 00 00 06 00 10 63 d9 97 3a 71 97 69 d1 15 8c 48 e5 9a 10 1e c9, SW1: 90, SW2: 00
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 6d 05 40 14 80 00 00 54 80 00 00 00 80 00 00 00 00 00 00 00 00 00 00 00
ENCRYPTION: GET_EXTENDED_KEY
ENCRYPTED: APDU: b0 82 00 00 48 e0 21 42 9b 37 96 d3 f4 41 33 00 41 00 00 00 07 00 20 54 94 a8 a2 d2 2f 09 bd b0 52 5c 44 f0 0a ef 1c 3c ee 7d 3c 8b f1 52 28 1c 28 4d 35 1a 4f a4 ba 00 14 d8 53 c8 94 b3 c7 4f cc dd 26 c9 24 fb d3 d7 c2 f9 71 ff 08
APDU: Response: 67 6e 00 01 39 6e 23 96 d9 56 2b 21 00 00 00 08 00 e0 2f 41 43 53 11 6d 3f c4 5c 59 bc 35 7d b2 bd b2 83 94 ff 45 c5 27 df 4e 34 9b a7 c6 39 7e d6 1c b4 d3 d6 84 e6 36 93 7e 57 e2 fd 5b 51 ed 1a 5d be cd d9 7c 69 99 8c f7 96 86 d3 a4 45 1a 4f 48 ff 75 0f a0 57 73 d5 98 88 f1 cf 75 c9 49 cb b9 2d 78 62 c3 87 47 bb df ca 1b 66 09 d8 1e 8e ee ac f9 a2 de e3 50 e7 f8 cb 97 08 f2 ed ed 68 79 06 07 8c 20 a4 97 08 fb b0 7a 01 05 12 b4 df 25 e0 fd af 20 9e 69 02 a6 c4 5c f6 1d 5c 29 cf c6 fa 31 10 d3 19 f9 16 46 d8 b0 ae fd a3 31 4b 09 9e fb 15 8c 13 04 c6 e3 13 0d 37 5e 67 55 b0 f0 ea 63 87 d7 31 82 19 d3 c1 62 9b 99 85 4c 91 f8 ec fb 39 5e 8d 75 a0 b0 58 fc 05 d0 8e 77 bd 3b 9b b4 fc 40 54 72 9e 2c 93 43 41 74 70 5a cd 87, SW1: 90, SW2: 00
PreSign: keyslot: 255, pubkey: <pysatochip.ecc.ECPubkey object at 0x102d51250>, message_byte: b'hello, this is a signature test.', hmac: b''
In card_sign_message
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 6e ff 01 04 00 00 00 20
ENCRYPTION: SIGN_MESSAGE
ENCRYPTED: APDU: b0 82 00 00 38 d5 55 34 57 07 32 eb a8 71 f5 e8 d4 00 00 00 09 00 10 2f 5f a4 8e 54 2f 8e 65 ab 24 01 f2 1d 98 ae c5 00 14 d7 6d f9 59 66 63 68 11 3c 5e 41 d6 7d 49 16 35 dd 96 a5 6e
APDU: Response: , SW1: 90, SW2: 00
ATTEMPT: select_apdu: 00 a4 04 00 0a a0 00 00 00 62 03 01 0c 06 01
APDU: plain_apdu: b0 6e ff 03 22 00 20 68 65 6c 6c 6f 2c 20 74 68 69 73 20 69 73 20 61 20 73 69 67 6e 61 74 75 72 65 20 74 65 73 74 2e
ENCRYPTION: SIGN_MESSAGE
ENCRYPTED: APDU: b0 82 00 00 58 58 b8 36 22 16 2a 09 11 49 80 d8 ca 00 00 00 0b 00 30 ba 1d 2d 41 c7 6b db 29 c7 1b 15 94 3e f1 cb f4 d2 d1 84 5d 9f a8 c4 c1 e6 1f 8e 27 09 d9 ab cb 05 72 33 0a 77 7c 31 91 35 7c 52 4e 93 74 96 6c 00 14 d5 a7 07 5b 37 9e c2 60 bd 92 cf f1 eb ff 1d 3f c7 ea e6 b0
APDU: Response: 21 e7 6d ea 1f 00 76 62 de 99 14 ed 00 00 00 0c 00 50 fd ec 60 94 fb 8a f4 05 19 e8 6c bb 05 76 80 91 3d e7 8f 5c f8 fa 4a 4f e0 fc a3 51 b4 31 2d 00 48 7d 75 00 b0 6d 68 58 5f 1f 3a 73 c7 64 62 f5 cb 93 57 cb bb b2 c7 6b 1b 86 8a dc 43 06 a4 2f 37 15 70 28 88 86 2b 7d 3c ca 56 0e cc 78 2d 29, SW1: 90, SW2: 00
DEBUG: hash: b"P\xcbl\xef\x94y\x1c*\xa2U\xa5\xae-\xd5\x07\xe0\xc7\x98\xf0\x06*'!2\xca\xdbl4\xa1\xf2+@"
Signature (Base64): H4Je2TixQOUJ5L4Oou9KMhEwCuR4yTpa1EODp70sey5FlVIW+thuUZUj1D0dGKochX/UKlqfCSLibiNtkHPdGhQ=
Message length: 32 bytes
Seperator length: 1 bytes
Signature length: 65 bytes
b'hello, this is a signature test.' : 1f825ed938b140e509e4be0ea2ef4a3211300ae478c93a5ad44383a7bd2c7b2e45955216fad86e519523d43d1d18aa1c857fd42a5a9f0922e26e236d9073dd1a14 98 bytes
.venvZilla:pysatochip i830671$ 