package com.github.thesmileydroid.rfidreader

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import java.nio.charset.Charset

class MifareTagTester {

    fun writeTag(tag: Tag, tagText: String, i: Int) : String? {
        MifareClassic.get(tag)?.use { classic ->
            classic.connect() // Conectar à tag
            if (classic.isConnected) { // Se a tag estiver conectada
                if (i > classic.sectorCount) { // Se o setor não existir
                    throw Exception("Sector $i does not exist")
                }
                Log.d("nfc", "Writing to sector $i")
                if (classic.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) { // Autenticar o setor
                    Log.d("nfc", "Authenticated sector $i")
                    val blockIndex = classic.sectorToBlock(i) // Obter o índice do bloco
                    // Complete the block with spaces
                    val blockStringPadded = tagText.padEnd(MifareClassic.BLOCK_SIZE, ' ') // Completar o bloco com espaços
                    val blockBytes = blockStringPadded.toByteArray(Charsets.US_ASCII) // Converter o bloco para bytes
                    Log.d("nfc", "Writing $tagText to block $blockIndex as bytes $blockBytes")
                    classic.writeBlock(blockIndex, blockBytes) // Escrever o bloco
                    return "Writing $tagText to block $blockIndex as bytes ${ 
                        blockBytes.toString(
                            Charsets.US_ASCII // Converter o bloco para string
                        )
                    }"
                }
            }
        }
        return null
    }

    fun readTag(tag: Tag): String? {
        return MifareClassic.get(tag)?.use { mifare ->
            mifare.connect() // Conectar à tag
            var result = ""
            if (mifare.isConnected) { // Se a tag estiver conectada
                for (i in 0 until mifare.sectorCount) { // Para cada setor
                    if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) { // Autenticar o setor
                        val blockIndex = mifare.sectorToBlock(i) // Obter o índice do bloco
                        val block = mifare.readBlock(blockIndex) // Ler o bloco
                        val blockString = String(block, Charset.forName("US-ASCII")) // Converter o bloco para string
                        result += "block: $blockIndex -> $blockString\n" // Adicionar o bloco à string de resultado
                    }
                }
                return result // Retornar o resultado
            } else {
                null
            }
        }
    }

    // Obter informações da tag (Coletar essas informações não causam nenhuma atividade na tag e não são bloqueantes)
    fun getInfo(tag: Tag): String? {
        return MifareClassic.get(tag)?.use { mifare ->
            val type = mifare.type
            val size = mifare.size
            val sectorCount = mifare.sectorCount
            val blockCount = mifare.blockCount

            return "Type: $type\nSize: $size\nSector Count: $sectorCount\nBlock Count: $blockCount"
        }
    }

    // Transforma o ID da tag em uma string hexadecimal
    fun getUUID(tag: Tag): String? {
        // Usa o MifareClassic para obter o ID da tag
        return MifareClassic.get(tag)?.use { mifare ->
            return mifare.tag.id.joinToString("") { "%02X".format(it) }
        }
    }
}