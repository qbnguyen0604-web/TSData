package vn.jupiter.tsdata.view

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import tornadofx.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// ========================================================
// 1. MODEL NỘI SOI (Lưu lại vị trí gốc của món đồ trong File)
// ========================================================
class GSItem(val fileOffset: Int, v0: Int, v2: Int, v4: Int, v6: Int, v8: Int, v10: Int) {
    val val0Prop = SimpleStringProperty(v0.toString())
    val val2Prop = SimpleStringProperty(v2.toString())
    val val4Prop = SimpleStringProperty(v4.toString())
    val val6Prop = SimpleStringProperty(v6.toString())
    val val8Prop = SimpleStringProperty(v8.toString())
    val val10Prop = SimpleStringProperty(v10.toString())
}

// ========================================================
// 2. CONTROLLER THÔNG MINH (Chỉ tìm và đè đúng 12 Byte, không làm hỏng cấu trúc)
// ========================================================
class GSTabController : Controller() {
    val items = FXCollections.observableArrayList<GSItem>()
    var originalBytes: ByteArray = ByteArray(0)

    fun loadData(file: File) {
        originalBytes = file.readBytes()
        val tempList = mutableListOf<GSItem>()
        
        // Quét toàn bộ file để tìm chữ ký A5 C9 (Bắt đầu 1 món đồ)
        for (i in 0 until originalBytes.size - 11) {
            if (originalBytes[i] == 0xA5.toByte() && originalBytes[i+1] == 0xC9.toByte()) {
                // Cắt đúng 12 Byte ra để đọc
                val buffer = ByteBuffer.wrap(originalBytes, i, 12).order(ByteOrder.LITTLE_ENDIAN)
                val v0 = buffer.short.toInt() and 0xFFFF
                val v2 = buffer.short.toInt() and 0xFFFF
                val v4 = buffer.short.toInt() and 0xFFFF
                val v6 = buffer.short.toInt() and 0xFFFF
                val v8 = buffer.short.toInt() and 0xFFFF
                val v10 = buffer.short.toInt() and 0xFFFF
                
                tempList.add(GSItem(i, v0, v2, v4, v6, v8, v10))
            }
        }
        
        Platform.runLater {
            items.clear()
            items.addAll(tempList)
        }
    }

    fun saveData(file: File) {
        // Copy lại file gốc, chỉ đè vào những chỗ đã sửa
        val outBytes = originalBytes.clone()
        
        for (item in items) {
            val buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putShort((item.val0Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val2Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val4Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val6Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val8Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val10Prop.value?.toIntOrNull() ?: 0).toShort())
            
            // Ghi đè 12 byte mới vào đúng tọa độ gốc của file
            System.arraycopy(buffer.array(), 0, outBytes, item.fileOffset, 12)
        }
        file.writeBytes(outBytes)
    }
}

// ========================================================
// 3. GIAO DIỆN BẢNG EXCEL
// ========================================================
class GSTabView : View("Shop Point (GS.dat)") {
    val controller: GSTabController by inject()

    override val root = borderpane {
        top = hbox(10) {
            paddingAll = 10.0
            alignment = Pos.CENTER_LEFT
            button("Load GS.dat") {
                action {
                    val files = chooseFile("Chọn GS.dat", arrayOf(javafx.stage.FileChooser.ExtensionFilter("DAT", "*.dat", "*.Dat")), mode = FileChooserMode.Single)
                    if (files.isNotEmpty()) {
                        runAsync {
                            controller.loadData(files[0])
                        }
                    }
                }
            }
            button("Save GS.dat") {
                action {
                    val files = chooseFile("Lưu GS.dat", arrayOf(javafx.stage.FileChooser.ExtensionFilter("DAT", "*.dat", "*.Dat")), mode = FileChooserMode.Save)
                    if (files.isNotEmpty()) {
                        runAsync {
                            controller.saveData(files[0])
                        } ui {
                            information("Thành công", "Đã lưu GS.dat thành công! Hãy ném vào Client ngay!")
                        }
                    }
                }
            }
        }
        
        center = tableview<GSItem>(controller.items) {
            isEditable = true
            columnResizePolicy = SmartResize.POLICY
            
            column("Offset 0 (Header)", GSItem::val0Prop).makeEditable()
            column("Offset 2 (Index)", GSItem::val2Prop).makeEditable()
            column("Offset 4 (Item Code)", GSItem::val4Prop).makeEditable()
            column("Offset 6 (Unknown)", GSItem::val6Prop).makeEditable()
            column("Offset 8 (Price Code)", GSItem::val8Prop).makeEditable()
            column("Offset 10 (Số lượng)", GSItem::val10Prop).makeEditable()
        }
    }
}
