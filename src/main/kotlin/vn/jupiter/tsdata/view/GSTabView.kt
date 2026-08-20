package vn.jupiter.tsdata.view

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import tornadofx.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// ========================================================
// 1. MODEL ĐỘC LẬP (Không kế thừa TSModel để tránh lỗi Private Init)
// ========================================================
class GSItem(v0: Int, v2: Int, v4: Int, v6: Int, v8: Int, v10: Int, v12: Int, v14: Int, v16: Int, v18: Int) {
    val val0Prop = SimpleIntegerProperty(v0)
    var val0 by val0Prop

    val val2Prop = SimpleIntegerProperty(v2)
    var val2 by val2Prop

    val val4Prop = SimpleIntegerProperty(v4)
    var val4 by val4Prop

    val val6Prop = SimpleIntegerProperty(v6)
    var val6 by val6Prop

    val val8Prop = SimpleIntegerProperty(v8)
    var val8 by val8Prop

    val val10Prop = SimpleIntegerProperty(v10)
    var val10 by val10Prop

    val val12Prop = SimpleIntegerProperty(v12)
    var val12 by val12Prop

    val val14Prop = SimpleIntegerProperty(v14)
    var val14 by val14Prop

    val val16Prop = SimpleIntegerProperty(v16)
    var val16 by val16Prop

    val val18Prop = SimpleIntegerProperty(v18)
    var val18 by val18Prop
}

// ========================================================
// 2. CONTROLLER ĐỌC/GHI TRỰC TIẾP BYTE (Bỏ qua DataRepo)
// ========================================================
class GSTabController : Controller() {
    val items = observableListOf<GSItem>()

    fun loadData(file: File) {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val tempList = mutableListOf<GSItem>()
        
        while (buffer.remaining() >= 20) {
            // Đọc 2 byte và dùng & 0xFFFF để ép kiểu sang số dương (chuẩn của TS)
            val v0 = buffer.short.toInt() and 0xFFFF
            val v2 = buffer.short.toInt() and 0xFFFF
            val v4 = buffer.short.toInt() and 0xFFFF
            val v6 = buffer.short.toInt() and 0xFFFF
            val v8 = buffer.short.toInt() and 0xFFFF
            val v10 = buffer.short.toInt() and 0xFFFF
            val v12 = buffer.short.toInt() and 0xFFFF
            val v14 = buffer.short.toInt() and 0xFFFF
            val v16 = buffer.short.toInt() and 0xFFFF
            val v18 = buffer.short.toInt() and 0xFFFF
            
            // Chỉ đưa vào bảng nếu dòng đó không phải là khoảng trống (Tránh rác)
            if (v0 != 0 || v2 != 0) {
                tempList.add(GSItem(v0, v2, v4, v6, v8, v10, v12, v14, v16, v18))
            }
        }
        
        runLater {
            items.clear()
            items.addAll(tempList)
        }
    }

    fun saveData(file: File) {
        val buffer = ByteBuffer.allocate(items.size * 20).order(ByteOrder.LITTLE_ENDIAN)
        for (item in items) {
            buffer.putShort(item.val0.toShort())
            buffer.putShort(item.val2.toShort())
            buffer.putShort(item.val4.toShort())
            buffer.putShort(item.val6.toShort())
            buffer.putShort(item.val8.toShort())
            buffer.putShort(item.val10.toShort())
            buffer.putShort(item.val12.toShort())
            buffer.putShort(item.val14.toShort())
            buffer.putShort(item.val16.toShort())
            buffer.putShort(item.val18.toShort())
        }
        file.writeBytes(buffer.array())
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
        
        center = tableview(controller.items) {
            isEditable = true
            columnResizePolicy = SmartResize.POLICY
            
            // Gắn trực tiếp vào Prop để chỉnh sửa mượt mà
            column("Offset 0 (Item ID)", GSItem::val0Prop).makeEditable()
            column("Offset 2", GSItem::val2Prop).makeEditable()
            column("Offset 4", GSItem::val4Prop).makeEditable()
            column("Offset 6", GSItem::val6Prop).makeEditable()
            column("Offset 8", GSItem::val8Prop).makeEditable()
            column("Offset 10", GSItem::val10Prop).makeEditable()
            column("Offset 12", GSItem::val12Prop).makeEditable()
            column("Offset 14", GSItem::val14Prop).makeEditable()
            column("Offset 16 (Số Lượng)", GSItem::val16Prop).makeEditable()
            column("Offset 18", GSItem::val18Prop).makeEditable()
        }
    }
}
