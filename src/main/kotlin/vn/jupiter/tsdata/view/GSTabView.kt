package vn.jupiter.tsdata.view

import javafx.geometry.Pos
import tornadofx.*
import vn.jupiter.tsdata.controller.DataRepo
import vn.jupiter.tsdata.data.TSModel
import java.nio.ByteBuffer
import java.nio.charset.Charset

// 1. CHẾ TẠO MODEL GIẢI MÃ 20 BYTE THÀNH 10 CỘT
class GSItem(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    var val0: Int get() = readShort(0); set(v) { saveShort(v, 0); id = v }
    var val2: Int get() = readShort(2); set(v) = saveShort(v, 2)
    var val4: Int get() = readShort(4); set(v) = saveShort(v, 4)
    var val6: Int get() = readShort(6); set(v) = saveShort(v, 6)
    var val8: Int get() = readShort(8); set(v) = saveShort(v, 8)
    var val10: Int get() = readShort(10); set(v) = saveShort(v, 10)
    var val12: Int get() = readShort(12); set(v) = saveShort(v, 12)
    var val14: Int get() = readShort(14); set(v) = saveShort(v, 14)
    var val16: Int get() = readShort(16); set(v) = saveShort(v, 16)
    var val18: Int get() = readShort(18); set(v) = saveShort(v, 18)

    init { id = val0 }
    override var name: String get() = ""; set(v) {}
    override var description: String get() = ""; set(v) {}
    override fun saveId(newId: Int) { val0 = newId; id = newId }
}

// 2. ÉP CỨNG THUẬT TOÁN ĐỌC 20 BYTE
class GSDataRepo : DataRepo<GSItem>(headerSize = 0, itemSize = 20) {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): GSItem {
        return GSItem(byteBuffer, itemSize, charSet)
    }
}

// 3. TẠO BỘ ĐIỀU KHIỂN (Ép kiểu tường minh để sửa lỗi Unresolved reference)
class GSTabController : Controller() {
    val repo = GSDataRepo()
    val items = observableListOf<GSItem>()

    fun loadData(file: java.io.File) {
        items.clear()
        val loaded = repo.load(file.absolutePath)
        
        // Nhận diện an toàn Map từ DataRepo để tránh lỗi "it"
        if (loaded is Map<*, *>) {
            val values = loaded.values as Collection<GSItem>
            items.addAll(values.filter { it.val0 != 0 || it.val2 != 0 })
        } else if (loaded is Collection<*>) {
            val values = loaded as Collection<GSItem>
            items.addAll(values.filter { it.val0 != 0 || it.val2 != 0 })
        }
    }

    fun saveData(file: java.io.File) {
        // Đóng gói thành Map chuẩn xác để fix lỗi Properties.save()
        val dataMap: Map<Int, GSItem> = items.associateBy { it.id }
        repo.save(file.absolutePath, dataMap)
    }
}

// 4. VẼ GIAO DIỆN BẢNG EXCEL
class GSTabView : View("Shop Point (GS.dat)") {
    val controller: GSTabController by inject()

    override val root = borderpane {
        top = hbox(10) {
            paddingAll = 10.0
            alignment = Pos.CENTER_LEFT
            button("Load GS.dat") {
                action {
                    // Fix lỗi FileChooserMode
                    val files = chooseFile("Chọn GS.dat", arrayOf(javafx.stage.FileChooser.ExtensionFilter("DAT", "*.dat", "*.Dat")), mode = FileChooserMode.Single)
                    if (files.isNotEmpty()) controller.loadData(files[0])
                }
            }
            button("Save GS.dat") {
                action {
                    // Fix lỗi FileChooserMode
                    val files = chooseFile("Lưu GS.dat", arrayOf(javafx.stage.FileChooser.ExtensionFilter("DAT", "*.dat", "*.Dat")), mode = FileChooserMode.Save)
                    if (files.isNotEmpty()) {
                        controller.saveData(files[0])
                        information("Thành công", "Đã lưu GS.dat thành công!")
                    }
                }
            }
        }
        
        // Khai báo kiểu <GSItem> để fix dứt điểm lỗi Type inference failed
        center = tableview<GSItem>(controller.items) {
            isEditable = true
            columnResizePolicy = javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
            
            column("Offset 0", GSItem::val0).makeEditable()
            column("Offset 2", GSItem::val2).makeEditable()
            column("Offset 4", GSItem::val4).makeEditable()
            column("Offset 6", GSItem::val6).makeEditable()
            column("Offset 8", GSItem::val8).makeEditable()
            column("Offset 10", GSItem::val10).makeEditable()
            column("Offset 12", GSItem::val12).makeEditable()
            column("Offset 14", GSItem::val14).makeEditable()
            column("Offset 16", GSItem::val16).makeEditable()
            column("Offset 18", GSItem::val18).makeEditable()
        }
    }
}
