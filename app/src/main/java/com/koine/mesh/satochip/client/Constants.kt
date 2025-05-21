package com.koine.mesh.satochip.client

object Constants {
    // APDU constants
    const val CLA_ISO7816 = 0x00.toByte()
    const val INS_SELECT = 0xA4.toByte()
    const val P1_SELECT_BY_NAME = 0x04.toByte()
    const val P2_SELECT_FIRST_OR_ONLY = 0x00.toByte()
    
    // Satochip specific constants
    // const val CLA_SATOCHIP = 0x80.toByte()
    const val CLA_SATOCHIP = 0xB0.toByte()
    const val INS_GET_STATUS = 0xF2.toByte()
    // const val INS_VERIFY_PIN = 0x20.toByte()
    const val INS_VERIFY_PIN = 0x42.toByte()
    const val INS_CHANGE_PIN = 0x21.toByte()
    const val INS_UNBLOCK_PIN = 0x22.toByte()
    const val INS_LOAD_KEY = 0xD0.toByte()
    const val INS_DERIVE_KEY = 0xD1.toByte()
    const val INS_GEN_KEY = 0xD2.toByte()
    // const val INS_SIGN = 0xC0.toByte()
    const val INS_SIGN = 0x6F.toByte()
    const val INS_GET_PUBKEY = 0xC1.toByte()

    // public static final byte INS_CREATE_PIN = (byte) 0x40; //TODO: remove?
    // public static final byte INS_VERIFY_PIN = (byte) 0x42;
    // public static final byte INS_CHANGE_PIN = (byte) 0x44;
    // public static final byte INS_UNBLOCK_PIN = (byte) 0x46;
    // public static final byte INS_LOGOUT_ALL = (byte) 0x60;

    // // Status information
    // public static final byte INS_LIST_PINS = (byte) 0x48;
    // public static final byte INS_GET_STATUS = (byte) 0x3C;
    // public static final byte INS_CARD_LABEL = (byte) 0x3D;
    // // HD wallet
    // public static final byte INS_BIP32_IMPORT_SEED = (byte) 0x6C;
    // public static final byte INS_BIP32_RESET_SEED = (byte) 0x77;
    // public static final byte INS_BIP32_GET_AUTHENTIKEY = (byte) 0x73;
    // public static final byte INS_BIP32_SET_AUTHENTIKEY_PUBKEY = (byte) 0x75;
    // public static final byte INS_BIP32_GET_EXTENDED_KEY = (byte) 0x6D;
    // public static final byte INS_BIP32_SET_EXTENDED_PUBKEY = (byte) 0x74;
    // public static final byte INS_SIGN_MESSAGE = (byte) 0x6E;
    // public static final byte INS_SIGN_SHORT_MESSAGE = (byte) 0x72;
    // public static final byte INS_SIGN_TRANSACTION = (byte) 0x6F;
    // public static final byte INS_PARSE_TRANSACTION = (byte) 0x71;
    // public static final byte INS_CRYPT_TRANSACTION_2FA = (byte) 0x76;
    // public static final byte INS_SET_2FA_KEY = (byte) 0x79;
    // public static final byte INS_RESET_2FA_KEY = (byte) 0x78;
    // public static final byte INS_SIGN_TRANSACTION_HASH = (byte) 0x7A;
    // // secure channel
    // public static final byte INS_INIT_SECURE_CHANNEL = (byte) 0x81;
    // public static final byte INS_PROCESS_SECURE_CHANNEL = (byte) 0x82;

    
    // Status words
    const val SW_SUCCESS = 0x9000
    const val SW_WRONG_PIN = 0x63C0
    const val SW_PIN_BLOCKED = 0x63C1
    const val SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982
    const val SW_CONDITIONS_NOT_SATISFIED = 0x6985
    const val SW_WRONG_DATA = 0x6A80
    const val SW_WRONG_LENGTH = 0x6700
    const val SW_INS_NOT_SUPPORTED = 0x6D00
    const val SW_CLA_NOT_SUPPORTED = 0x6E00
    
    // Key types
    const val KEY_TYPE_MASTER = 0x00.toByte()
    const val KEY_TYPE_AUTHENTICATION = 0x01.toByte()
    const val KEY_TYPE_ENCRYPTION = 0x02.toByte()
    const val KEY_TYPE_SIGNATURE = 0x03.toByte()
    
    // PIN constants
    const val PIN_DEFAULT = "123456"
    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 32
    const val PIN_MAX_TRIES = 3
} 