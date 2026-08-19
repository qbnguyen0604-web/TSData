package vn.jupiter.tsdata.controller

import tornadofx.Controller
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmgPatcherController : Controller() {

    class MapHeader(val mapId: String, var offset: Int, var length: Int, val headerPos: Int, val origOffset: Int, val origLength: Int)

    fun injectShopToEmg(emgFile: File, outputFile: File, mapIdTarget: String, shopId: Byte, itemIds: List<Int>) {
        val raf = RandomAccessFile(emgFile, "r")
        val totalSize = raf.length().toInt()
        val fullData = ByteArray(totalSize)
        raf.readFully(fullData)
        raf.close()

        val buffer = ByteBuffer.wrap(fullData).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(2)

        val headers = mutableListOf<MapHeader>()
        var endHeader = totalSize

        // 1. ĐỌC MỤC LỤC
        while (buffer.position() < endHeader) {
            val pos = buffer.position()
            val mapIdBytes = ByteArray(5)
            buffer.position(pos + 1)
            buffer.get(mapIdBytes)
            val mapId = String(mapIdBytes).trim()

            buffer.position(pos + 24)
            val offset = buffer.int
            val length = buffer.int

            if (endHeader == totalSize) endHeader = offset

            headers.add(MapHeader(mapId, offset, length, pos, offset, length))
            buffer.position(pos + 32)
        }

        // 2. TÌM MAP MỤC TIÊU
        val targetIdx = headers.indexOfFirst { it.mapId == mapIdTarget }
        if (targetIdx == -1) throw Exception("Không tìm thấy Map $mapIdTarget trong file EMG!")

        val targetHeader = headers[targetIdx]
        val mapData = ByteArray(targetHeader.length)
        System.arraycopy(fullData, targetHeader.origOffset, mapData, 0, targetHeader.length)

        // 3. DÒ TÌM VỊ TRÍ KHỐI SHOP TRONG MAP (Dịch chuẩn xác 100% từ C#)
        var p = 103
        val nb_npc = mapData[p].toInt() and 0xFF
        p += 4
        for (i in 0 until nb_npc) {
            p += 6
            // FIX LỖI CRASH TẠI ĐÂY: Đọc từ p - 2
            val nb = (mapData[p - 2].toInt() and 0xFF) or ((mapData[p - 1].toInt() and 0xFF) shl 8)
            p += nb
            val b = mapData[p].toInt() and 0xFF; p++
            p += b
            val b2 = mapData[p].toInt() and 0xFF; p++
            p += 8
            p += b2 * 8
            p += 43
            p += 37
        }
        val nb_items = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
        p++ // C# dùng p++ ở đây
        for (j in 0 until nb_items) {
            p += 13
        }
        p++ // C# dùng p++ ở đây

        val nb_gates = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
        p += 2
        for (k in 0 until nb_gates) {
            p += 2
            val nb2 = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
            p += 2
            p += nb2
            p += 21
        }
        val nb_enc = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
        p += 2
        for (l in 0 until nb_enc) {
            p += 2
            val numCount = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
            p += 2
            p += numCount
            p += 16
            p++
        }

        val dialogCountOffset = p
        val nb_dialog = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
        p += 2

        for (m in 0 until nb_dialog) {
            p += 4
            val nb_d = mapData[p].toInt() and 0xFF
            p += 4
            p += 5 * nb_d
        }

        val insertPoint = p 

        // 4. CHẾ TẠO GÓI TIN SHOP VIP
        val nb_d_new = itemIds.size + 1
        val payload = ByteBuffer.allocate(8 + 5 * nb_d_new).order(ByteOrder.LITTLE_ENDIAN)
        payload.put(shopId)
        payload.put(ByteArray(3))
        payload.put(nb_d_new.toByte())
        payload.put(ByteArray(2))
        payload.put(1.toByte()) 
        for (id in itemIds) {
            payload.putShort(id.toShort())
            payload.put(ByteArray(3))
        }
        payload.put(ByteArray(5)) 
        val payloadBytes = payload.array()

        // 5. CẬP NHẬT SỐ LƯỢNG VÀ LẮP RÁP MAP
        val newDialogCount = nb_dialog + 1
        mapData[dialogCountOffset] = (newDialogCount and 0xFF).toByte()
        mapData[dialogCountOffset+1] = ((newDialogCount shr 8) and 0xFF).toByte()

        val newMapData = ByteArray(mapData.size + payloadBytes.size)
        System.arraycopy(mapData, 0, newMapData, 0, insertPoint)
        System.arraycopy(payloadBytes, 0, newMapData, insertPoint, payloadBytes.size)
        System.arraycopy(mapData, insertPoint, newMapData, insertPoint + payloadBytes.size, mapData.size - insertPoint)

        // 6. CẬP NHẬT LẠI OFFSETS CHO CÁC MAP PHÍA SAU
        val sizeDiff = payloadBytes.size
        headers[targetIdx].length = newMapData.size
        for (i in (targetIdx + 1) until headers.size) {
            headers[i].offset += sizeDiff
        }

        // 7. XUẤT RA FILE MỚI
        val out = FileOutputStream(outputFile)
        val headerBuffer = ByteBuffer.allocate(endHeader).order(ByteOrder.LITTLE_ENDIAN)
        System.arraycopy(fullData, 0, headerBuffer.array(), 0, endHeader)
        for (h in headers) {
            headerBuffer.position(h.headerPos + 24)
            headerBuffer.putInt(h.offset)
            headerBuffer.putInt(h.length)
        }
        out.write(headerBuffer.array())

        for (i in headers.indices) {
            if (i == targetIdx) {
                out.write(newMapData)
            } else {
                out.write(fullData, headers[i].origOffset, headers[i].origLength)
            }
        }
        out.close()
    }
}
