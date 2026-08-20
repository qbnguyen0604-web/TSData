package vn.jupiter.tsdata.controller

import tornadofx.Controller
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmgPatcherController : Controller() {

    class MapHeader(val mapId: String, var offset: Int, var length: Int, val headerPos: Int, val origOffset: Int, val origLength: Int)

    fun injectShopToEmg(emgFile: File, outputFile: File, mapIdTarget: String, targetShopId: Byte, itemIds: List<Int>) {
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

        // 3. NHẢY QUA CÁC KHỐI DỮ LIỆU ĐỂ TÌM SHOP
        var p = 103
        val nb_npc = mapData[p].toInt() and 0xFF
        p += 4
        for (i in 0 until nb_npc) {
            p += 6
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
        p++ 
        for (j in 0 until nb_items) p += 13
        p++ 

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

        // 4. BẮT ĐẦU ĐOẠT XÁ (TÌM VÀ GHI ĐÈ SHOP)
        val nb_dialog = (mapData[p].toInt() and 0xFF) or ((mapData[p+1].toInt() and 0xFF) shl 8)
        p += 2

        var found = false
        var oldShopStart = 0
        var oldShopEnd = 0

        for (m in 0 until nb_dialog) {
            val startOfDialog = p
            val currentShopId = mapData[p]
            p += 4
            val nb_d = mapData[p].toInt() and 0xFF
            p += 4
            val blockLength = 5 * nb_d

            // Nếu tìm thấy đúng ID Shop cần ghi đè
            if (currentShopId == targetShopId) {
                found = true
                oldShopStart = startOfDialog
                oldShopEnd = p + blockLength
                break
            }
            p += blockLength
        }

        if (!found) throw Exception("Không tìm thấy Shop ID $targetShopId trong Map $mapIdTarget!")

        // 5. CHẾ TẠO SHOP VIP MỚI
        val nb_d_new = itemIds.size + 1
        val payload = ByteBuffer.allocate(8 + 5 * nb_d_new).order(ByteOrder.LITTLE_ENDIAN)
        payload.put(targetShopId)
        payload.put(ByteArray(3))
        payload.put(nb_d_new.toByte())
        payload.put(ByteArray(2))
        payload.put(1.toByte()) // Cờ đánh dấu là Shop
        for (id in itemIds) {
            payload.putShort(id.toShort())
            payload.put(ByteArray(3))
        }
        payload.put(ByteArray(5)) 
        val payloadBytes = payload.array()

        // 6. LẮP RÁP LẠI MAP VÀ CẬP NHẬT HEADER
        val newMapData = ByteArray(mapData.size - (oldShopEnd - oldShopStart) + payloadBytes.size)
        System.arraycopy(mapData, 0, newMapData, 0, oldShopStart)
        System.arraycopy(payloadBytes, 0, newMapData, oldShopStart, payloadBytes.size)
        System.arraycopy(mapData, oldShopEnd, newMapData, oldShopStart + payloadBytes.size, mapData.size - oldShopEnd)

        val sizeDiff = newMapData.size - mapData.size
        headers[targetIdx].length = newMapData.size
        for (i in (targetIdx + 1) until headers.size) {
            headers[i].offset += sizeDiff
        }

        // 7. XUẤT RA FILE eve.emg MỚI
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
            if (i == targetIdx) out.write(newMapData)
            else out.write(fullData, headers[i].origOffset, headers[i].origLength)
        }
        out.close()
    }
}
