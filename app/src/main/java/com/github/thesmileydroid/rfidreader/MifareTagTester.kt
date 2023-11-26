package com.github.thesmileydroid.rfidreader

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import java.nio.charset.Charset

class MifareTagTester {

    fun writeTag(tag: Tag, tagText: String, i: Int) : String? {
        MifareClassic.get(tag)?.use { classic ->
            classic.connect()
            if (classic.isConnected) {
                if (i > classic.sectorCount) {
                    throw Exception("Sector $i does not exist")
                }
                Log.d("nfc", "Writing to sector $i")
                if (classic.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                    Log.d("nfc", "Authenticated sector $i")
                    val blockIndex = classic.sectorToBlock(i)
                    val blockString = "$tagText"
                    // Complete the block with spaces
                    val blockStringPadded = blockString.padEnd(MifareClassic.BLOCK_SIZE, ' ')
                    val blockBytes = blockStringPadded.toByteArray(Charsets.US_ASCII)
                    Log.d("nfc", "Writing $blockString to block $blockIndex as bytes $blockBytes")
                    classic.writeBlock(blockIndex, blockBytes)
                    return "Writing $blockString to block $blockIndex as bytes ${blockBytes.toString(Charsets.US_ASCII)}"
                }
            }
        }
        return null
    }

    fun readTag(tag: Tag): String? {
        return MifareClassic.get(tag)?.use { mifare ->
            mifare.connect()
            var result = ""
            if (mifare.isConnected) {
                for (i in 0 until mifare.sectorCount) {
                    if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                        val blockIndex = mifare.sectorToBlock(i)
                        val block = mifare.readBlock(blockIndex)
                        val blockString = String(block, Charset.forName("US-ASCII"))
                        result += "$blockString\n"
                    }
                }

                return result
            } else {
                null
            }
        }
    }

    fun getInfo(tag: Tag): String? {
        return MifareClassic.get(tag)?.use { mifare ->
            val type = mifare.type
            val size = mifare.size
            val sectorCount = mifare.sectorCount
            val blockCount = mifare.blockCount

            return "Type: $type\nSize: $size\nSector Count: $sectorCount\nBlock Count: $blockCount"
        }
    }

    fun getUUID(tag: Tag): String? {
        return MifareClassic.get(tag)?.use { mifare ->
            val uuid = mifare.tag.id.joinToString("") { "%02X".format(it) }
            return uuid.toString()
        }
    }
}