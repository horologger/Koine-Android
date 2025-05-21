# Satochip Sign Message APDU Sequence

This document outlines the sequence of APDU commands used by the `satochip-sign-message` command.

## Initial Connection

1. **Protocol Selection**
   - Try T0 protocol (fails with 0x8010000F)
   - Successfully connect using T1 protocol

2. **Applet Selection**
   ```
   APDU: 00 A4 04 00 08 53 61 74 6F 43 68 69 70
   Response: [], SW1: 0x90, SW2: 0x00
   ```

3. **Get Card Status**
   ```
   APDU: B0 3C 00 00
   Response: [00 0C 00 05 05 01 01 01 00 01 01 01], SW1: 0x90, SW2: 0x00
   ```

4. **Initialize Secure Channel**
   ```
   APDU: B0 81 00 00 41 [encrypted data]
   Response: [encrypted data], SW1: 0x90, SW2: 0x00
   ```

## PIN Verification

5. **Verify PIN**
   ```
   APDU: 80 82 00 00 [length] [PIN bytes]
   Response: [encrypted data], SW1: 0x90, SW2: 0x00
   ```

## Message Signing

6. **Sign Message (First Chunk)**
   ```
   APDU: B0 82 00 00 [length] [encrypted message data]
   Response: [encrypted data], SW1: 0x90, SW2: 0x00
   ```

7. **Sign Message (Subsequent Chunks)**
   ```
   APDU: B0 82 00 00 [length] [encrypted message data]
   Response: [encrypted data], SW1: 0x90, SW2: 0x00
   ```
   (This step repeats for each chunk of the message)

8. **Final Sign Message**
   ```
   APDU: B0 82 00 00 [length] [encrypted final data]
   Response: [encrypted signature], SW1: 0x90, SW2: 0x00
   ```

## Notes

- All commands after secure channel initialization are encrypted
- The message is split into chunks if it exceeds the maximum APDU data length
- The final response contains the signature in Base64 format
- Status words (SW1, SW2) are used to indicate success (0x9000) or various error conditions

## Error Codes

- 0x8010000F: Protocol mismatch
- 0x9C21: Secure channel not initialized
- 0x6300-0x63C3: Wrong PIN (with remaining tries)
- 0x6983: PIN blocked
- 0x9C04: Setup not done 

## Example Successful Sign Message Output

DEBUG: Trying T1 protocol...
DEBUG: Successfully connected using T1 protocol
DEBUG: APDU: 00 a4 04 00 08 53 61 74 6f 43 68 69 70
DEBUG: Response: , SW1: 90, SW2: 00
DEBUG: APDU: b0 3c 00 00
DEBUG: Response: 00 0c 00 05 05 01 01 01 00 01 01 01, SW1: 90, SW2: 00
DEBUG: APDU: b0 81 00 00 41 04 0d e3 bb 89 c7 73 f9 8f de f1 a0 75 21 d0 02 69 ac 37 bf b6 cd 25 bb c3 d7 77 1c 26 a6 12 e8 c4 10 2f b5 03 bb ab fa 9f 14 fe 38 34 f1 ae 6e 4e e9 a3 1a 00 56 af d8 53 07 65 c3 0c ad 25 67 a7
DEBUG: Response: 00 20 57 d4 7e 94 df 9b f0 89 a6 f1 4d 39 e6 ee f7 bf 9c 4b 38 99 fc 85 a5 7e ca 48 b0 57 92 dc 2e b8 00 47 30 45 02 21 00 86 5e ad 6b 16 47 7c cf 14 3c e7 ca c2 56 e2 52 0a bb 1e 1b e6 d9 1a c4 e3 e3 f0 2b 9b 05 15 50 02 20 09 f0 3a 6a 5f cc 6a d1 f9 c0 d1 e5 90 91 18 57 60 79 c5 43 de 9b d5 75 1f 7a 3e 64 8c 97 b8 05 00 46 30 44 02 20 12 1b d4 37 b3 c0 4e 71 1c 61 32 73 6e a6 45 d2 61 05 83 53 b7 41 e9 23 86 db a7 dd 6a 95 6b 0d 02 20 10 91 01 be 66 d5 69 52 45 41 61 31 f6 c0 83 1b 15 83 d3 78 7d ec ae 48 f4 e6 d7 0c b2 35 1d 27, SW1: 90, SW2: 00
Enter your PIN:
DEBUG: APDU: b0 82 00 00 38 a6 96 ec 39 19 80 ed f9 bd c9 64 c7 00 00 00 03 00 10 5d 0f 57 03 7b fc 38 19 86 de e1 51 0f 92 02 65 00 14 10 b5 e4 5b c1 90 10 1b cc f6 2f b6 da 62 2d a5 a2 b8 d7 cb
DEBUG: Response: 80 c5 ca e9 1f 07 5a cc d0 39 a5 f4 00 00 00 04 00 70 ec 1a 03 79 8a 9d dc 87 7f 15 4b 78 e4 48 8c c7 50 df 38 f8 e0 00 3d c2 3f 63 97 ac 14 3b fc bf 4a 66 27 5c a3 2a 53 5b 64 a3 5a ee 20 ba 51 16 d8 e0 12 77 1a b1 38 ad 22 4e fb 59 5a 19 63 71 04 c5 6b 4e ad fd e7 ab 03 36 90 0d 47 c8 51 89 20 3c 6c 9c ec 45 ba c3 a3 92 38 44 67 8c 72 71 4e ed 7c b2 7f fc bb 9a ca a6 29 ee 7b 37 31 6e, SW1: 90, SW2: 00
DEBUG: APDU: b0 82 00 00 c8 40 4f 22 10 68 91 d9 13 df 0c c6 c4 00 00 00 05 00 a0 fe 69 d4 a5 9c 69 3b 50 79 ff 0e bf 98 f3 d9 25 2a 31 65 a3 12 d6 2d 83 a7 44 e0 68 26 eb a8 aa eb 53 ea 04 20 e1 b0 62 12 db c2 e9 a3 e1 90 d3 db 24 e0 6e 3a e2 d4 f9 68 b9 5b 0c 0d 96 38 d0 ae fd c6 97 01 dc 84 42 30 1f 62 e4 69 c3 db 42 94 31 2e d5 2f 25 d9 ba d7 ec 96 ab b7 fb b1 44 e6 e8 1c b1 e7 68 63 84 bc bf 35 7d b6 1f 89 3b 4e ef 72 37 d1 e2 1f 52 6f 51 2d 23 7a 6b ba 3d a3 9e dd 26 af f5 e9 1f 67 af ae f3 20 bf ff 02 fe 8d 34 85 0e 51 1c b4 7c 4f d3 81 ca bc 38 e4 00 14 df 2f 7d ad 8d 36 08 f8 96 07 d4 00 f2 e5 3b 3f 71 95 59 56
DEBUG: Response: 16 37 4d ad a8 eb 14 2d 9f 1a 70 96 00 00 00 06 00 10 81 31 2e 2d 57 23 da 79 ab cb 0a 88 84 f6 86 67, SW1: 90, SW2: 00
DEBUG: APDU: b0 82 00 00 48 cb bb 48 4a 5d b8 0d e5 86 76 ae ef 00 00 00 07 00 20 3f b8 5f e3 96 e0 91 ac 27 f5 3c dc 8a 1e 62 8d 5b db e0 3b 2e 8a 4f 85 f1 40 43 c4 47 6e ea ba 00 14 88 2c ca bb ae bd ab bd 65 9c 58 15 f3 d2 aa 2c 99 3b f3 0d
DEBUG: Response: 58 14 8b e5 cc ee c2 5a 29 27 ec e1 00 00 00 08 00 e0 74 93 ce 4d e7 07 5e c1 6c 7a 1f cb e3 8a cf 5c 86 0f 61 60 4f 73 e8 28 ae 22 63 92 f2 ba 20 94 e9 cf 63 da ba 09 aa ba e9 95 8c 50 9d df 1e c8 d2 57 ae b9 c1 9e e4 30 52 dc 76 56 ce 09 61 72 c5 14 bc ea 36 cc 2b 1f 2f ef d6 dc 5d 39 84 61 94 db bf 7c 85 0f f2 46 55 e2 dd 32 76 38 72 f2 82 58 58 36 ef 2a 19 35 2c 80 36 01 34 6c 2f 93 4b 12 c7 f2 08 e1 76 fd ff 87 26 62 6f ea bb 32 c7 83 53 9e 66 70 5e 4f 49 99 35 27 f4 a9 c2 69 ec e5 f2 f9 a5 9b 2b 5d 7c d4 63 c9 88 86 20 a1 80 c6 37 ac 4a 15 d4 b5 33 2a 4b 68 67 9f c8 2a 68 cc 0b 45 34 de 28 25 59 88 63 d8 0e cd 77 78 76 e4 fc 36 c5 e9 da c3 22 57 17 60 bd cd 16 a1 32 db 88 16 a3 32 6c e8 2f 32 a8 f3 90 d8 24 e3, SW1: 90, SW2: 00
DEBUG: APDU: b0 82 00 00 38 ee 6e 25 b1 8d fd 51 7b 67 76 ae e5 00 00 00 09 00 10 e0 e2 fc e9 a8 55 67 8f 11 80 6b d1 28 65 ae 20 00 14 c2 c8 6a 36 7a 86 bb c9 fb 59 e7 de 3b 90 d0 d4 01 26 dc e0
DEBUG: Response: , SW1: 90, SW2: 00
DEBUG: APDU: b0 82 00 00 38 b8 67 f3 e9 65 a7 c4 6f e9 23 ad 68 00 00 00 0b 00 10 c9 a0 f3 9c df 7d b7 49 cf da d2 8a 51 0a 76 e2 00 14 88 90 bd e0 e3 dd f1 55 33 3d 92 5b 52 0b 0b fb e0 64 b6 3d
DEBUG: Response: c6 d8 ee 17 b0 9a d6 e5 96 e6 d0 a9 00 00 00 0c 00 50 40 a5 ba 39 f8 29 3d 04 89 ca b5 72 34 21 73 c9 a6 55 ed 4c 9c 06 01 e9 d7 41 03 6f e4 3d 92 e0 ff 8b 9d 52 df c0 fa 5e 80 87 3a 30 79 11 cf 0c d2 b8 30 66 17 ce ab f8 f6 6e 12 f1 b4 e4 88 3c 44 b7 02 8b 59 38 87 35 04 dd b4 c9 27 65 0c 04, SW1: 90, SW2: 00
Signature (Base64): IPbe13P6jW0ajtPJzhDW4cmvZoHIRdelwK7QqPXo4iV2theRstISgKDBp5V6fuCEaX0HJTBVHn5UC0ruRoiZ1V4=
