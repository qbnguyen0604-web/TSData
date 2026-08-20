package vn.jupiter.tsdata.view

import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import tornadofx.*
import vn.jupiter.tsdata.controller.GSUnlockerController
import java.io.File

class GSUnlockerView : View("GS.dat Mass Unlocker") {
    val controller: GSUnlockerController by inject()
    var sourceFileTF: TextField by singleAssign()

    override val root = vbox(15) {
        paddingAll = 30.0
        alignment = Pos.TOP_CENTER

        label("CÔNG CỤ MỞ KHÓA VÔ HẠN SỐ LƯỢNG SHOP POINT") {
            style { fontSize = 20.px; fontWeight = javafx.scene.text.FontWeight.BOLD }
        }

        hbox(10) {
            alignment = Pos.CENTER_LEFT
            label("File GS.dat gốc (Từ Client):")
            sourceFileTF = textfield { hgrow = Priority.ALWAYS }
            button("Chọn File...") {
                action {
                    val files = chooseFile("Chọn file GS.dat", arrayOf(FileChooser.ExtensionFilter("DAT File", "*.Dat", "*.dat")))
                    if (files.isNotEmpty()) sourceFileTF.text = files[0].absolutePath
                }
            }
        }

        button("ÉP XUNG VÔ HẠN TOÀN BỘ SHOP") {
            prefWidth = Double.MAX_VALUE
            prefHeight = 50.0
            style { fontSize = 16.px; fontWeight = javafx.scene.text.FontWeight.BOLD; textFill = javafx.scene.paint.Color.RED }
            action {
                try {
                    val inFile = File(sourceFileTF.text)
                    if (!inFile.exists()) throw Exception("Vui lòng chọn file GS.dat hợp lệ!")
                    
                    val outFile = File(inFile.parentFile, "GS_mod.dat")
                    
                    runAsync {
                        controller.unlockInfiniteStock(inFile, outFile)
                    } ui { count ->
                        information("Thành Công Rực Rỡ!", "Đã bẻ khóa thành công $count vật phẩm thành VÔ HẠN!!\nFile xuất ra: ${outFile.name}\nHãy đổi tên nó thành GS.dat và chép đè vào Client!")
                    }
                } catch (e: Exception) {
                    error("Lỗi", e.message ?: "Lỗi không xác định!")
                }
            }
        }
    }
}
