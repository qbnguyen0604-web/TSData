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
// 1. MODEL ĐỘC LẬP (Ép dùng StringProperty để UI cho phép Edit mượt mà)
// ========================================================
class GSItem(v0: Int, v2: Int, v4: Int, v6: Int, v8: Int, v10: Int, v12: Int, v14: Int, v16: Int, v18: Int) {
    val val0Prop = SimpleStringProperty(v0.toString())
    val val2Prop = SimpleStringProperty(v2.toString())
    val val4Prop = SimpleStringProperty(v4.toString())
    val val6Prop = SimpleStringProperty(v6.toString())
    val val8Prop = SimpleStringProperty(v8.toString())
    val val10Prop = SimpleStringProperty(v10.toString())
    val val12Prop = SimpleStringProperty(v12.toString())
    val val14Prop = SimpleStringProperty(v14.toString())
    val val16Prop = SimpleStringProperty(v16.toString())
    val val18Prop = SimpleStringProperty(v18.toString())
}

// ========================================================
// 2. CONTROLLER ĐỌC/GHI TRỰC TIẾP BYTE
// ========================================================
class GSTabController : Controller() {
    val items = FXCollections.observableArrayList<GSItem>()

    fun loadData(file: File) {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val tempList = mutableListOf<GSItem>()
        
        while (buffer.remaining() >= 20) {
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
            
            // Chỉ hiển thị các món đồ có Data (Loại bỏ khoảng trống)
            if (v0 != 0 || v2 != 0) {
                tempList.add(GSItem(v0, v2, v4, v6, v8, v10, v12, v14, v16, v18))
            }
        }
        
        Platform.runLater {
            items.clear()
            items.addAll(tempList)
        }
    }

    fun saveData(file: File) {
        val buffer = ByteBuffer.allocate(items.size * 20).order(ByteOrder.LITTLE_ENDIAN)
        for (item in items) {
            // Tự động chuyển từ String trên UI về lại số Short để ghi vào File (Nếu rỗng thì tự gán = 0)
            buffer.putShort((item.val0Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val2Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val4Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val6Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val8Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val10Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val12Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val14Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val16Prop.value?.toIntOrNull() ?: 0).toShort())
            buffer.putShort((item.val18Prop.value?.toIntOrNull() ?: 0).toShort())
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
        
        center = tableview<GSItem>(controller.items) {
            isEditable = true
            columnResizePolicy = SmartResize.POLICY
            
            // Bây giờ Property là String nên TornadoFX sẽ tự động hiển thị TextBox chỉnh sửa
            column("Offset 0 (Item ID)", GSItem::val0Prop).makeEditable()
            column("Offset 2", GSItem::val2Prop).makeEditable()
            column("Offset 4", GSItem::val4Prop).makeEditable()
            column("Offset 6", GSItem::val6Prop).makeEditable()
            column("Offset 8", GSItem::val8Prop).makeEditable()
            column("Offset 10", GSItem::val10Prop).makeEditable()
            column("Offset 12", GSItem::val12Prop).makeEditable()
            column("Offset 14", GSItem::val14Prop).makeEditable()
            column("Offset 16 (Số lượng)", GSItem::val16Prop).makeEditable()
            column("Offset 18", GSItem::val18Prop).makeEditable()
        }
    }
}
