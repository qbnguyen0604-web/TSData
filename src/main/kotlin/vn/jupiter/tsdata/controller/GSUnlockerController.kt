package vn.jupiter.tsdata.controller

import tornadofx.Controller
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class GSUnlockerController : Controller() {

    fun unlockInfiniteStock(inFile: File, outFile: File): Int {
        val raf = RandomAccessFile(inFile, "r")
        val data = ByteArray(raf.length().toInt())
        raf.readFully(data)
        raf.close()

        var unlockedCount = 0

        // GS.dat cấu trúc mỗi block đồ dài chuẩn 24 byte
        for (i in 0 until data.size step 24) {
            // Kiểm tra an toàn: Phải đủ 24 byte và 4 byte đầu tiên phải là 00 00 00 00 (Tránh sửa nhầm Header)
            if (i + 23 < data.size && data[i] == 0.toByte() && data[i+1] == 0.toByte()) {
                
                // Ép xung: Dựa vào Hex, byte 16 đến 19 là khoảng trống của Số Lượng.
                // Ta set chúng thành 0xFF (-1) để ép UI Client hiển thị chữ "Vô hạn!!"
                data[i + 16] = 0xFF.toByte()
                data[i + 17] = 0xFF.toByte()
                data[i + 18] = 0xFF.toByte()
                data[i + 19] = 0xFF.toByte()
                
                unlockedCount++
            }
        }

        // Xuất file mới
        val out = FileOutputStream(outFile)
        out.write(data)
        out.close()

        return unlockedCount
    }
}
