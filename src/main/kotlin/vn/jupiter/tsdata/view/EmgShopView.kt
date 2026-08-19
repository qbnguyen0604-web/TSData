package vn.jupiter.tsdata.view

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import tornadofx.*
import vn.jupiter.tsdata.controller.EmgPatcherController
import java.io.File

class EmgShopView : View("EMG Shop Patcher") {
    val controller: EmgPatcherController by inject()

    var sourceFileTF: TextField by singleAssign()
    val mapIdProp = SimpleStringProperty("12001")
    val shopIdProp = SimpleStringProperty("99")
    var itemIdsTextArea: TextArea by singleAssign()

    override val root = vbox(10) {
        paddingAll = 20.0
        
        hbox(10) {
            alignment = Pos.CENTER_LEFT
            label("File eve_shopper.emg gốc:")
            sourceFileTF = textfield { hgrow = Priority.ALWAYS }
            button("Chọn File...") {
                action {
                    val files = chooseFile("Chọn eve_shopper.emg", arrayOf(FileChooser.ExtensionFilter("EMG File", "*.emg")))
                    if (files.isNotEmpty()) sourceFileTF.text = files[0].absolutePath
                }
            }
        }

        hbox(10) {
            alignment = Pos.CENTER_LEFT
            label("Bơm vào Map ID:")
            textfield(mapIdProp) { prefWidth = 100.0 }
            label("  |  ID Shop VIP:")
            textfield(shopIdProp) { prefWidth = 80.0 }
        }

        label("Danh sách Item ID (Mỗi ID một dòng, hoặc cách nhau bởi dấu phẩy):")
        itemIdsTextArea = textarea {
            vgrow = Priority.ALWAYS
            promptText = "Ví dụ: \n46070\n23086\n23087..."
        }

        button("BƠM SHOP VÀO FILE EMG") {
            prefWidth = Double.MAX_VALUE
            style { fontSize = 16.px; fontWeight = javafx.scene.text.FontWeight.BOLD }
            action {
                try {
                    val inFile = File(sourceFileTF.text)
                    if (!inFile.exists()) throw Exception("Chưa chọn file EMG hợp lệ!")
                    
                    val mapId = mapIdProp.value
                    val shopId = shopIdProp.value.toByte()
                    val itemIds = itemIdsTextArea.text.split(Regex("[,\\s\\n]+")).filter { it.isNotBlank() }.map { it.toInt() }

                    val outFile = File(inFile.parentFile, "eve_shopper_mod.emg")
                    
                    runAsync {
                        controller.injectShopToEmg(inFile, outFile, mapId, shopId, itemIds)
                    } ui {
                        information("Thành Công Rực Rỡ!", "Đã bơm xong ${itemIds.size} món đồ vào Shop $shopId tại Map $mapId.\nFile xuất ra: ${outFile.name}\nHãy đổi tên nó thành eve_shopper.emg và ném vào Client!")
                    }
                } catch (e: Exception) {
                    error("Lỗi sấp mặt", e.message ?: "Lỗi không xác định!")
                }
            }
        }
    }
}
