package vn.jupiter.tsdata.view

import javafx.stage.FileChooser
import tornadofx.*
import vn.jupiter.tsdata.controller.*
import vn.jupiter.tsdata.data.Item
import java.io.File

class MainView : View("TS Data Editor - Ultimate Version") {
    
    // Khởi tạo Controller riêng cho Item để dễ dàng trích xuất dữ liệu
    val itemRepo = ItemInfoDataRepo()
    val itemController = ItemTabController(itemRepo)
    
    val dataSet = mapOf(
            "Item" to ItemTabView(itemController),
            "NPC" to ItemTabView(ItemTabController(NpcInfoDataRepo(), NpcInfoDataRepo())),
            "Talk" to ItemTabView(ItemTabController(TalkDataRepo())),
            "Skill" to ItemTabView(ItemTabController(SkillDataRepo())),
            "Scene" to ItemTabView(ItemTabController(SceneSkillDataRepo()))
    )

    override val root = borderpane {
        top = menubar {
            menu("Xử lý Hàng Loạt (Excel/CSV)") {
                item("1. Xuất Item.Dat ra CSV").action { exportToCSV() }
                item("2. Nhập CSV vào Tool").action { importFromCSV() }
            }
        }
        center = tabpane {
            dataSet.keys.forEach {
                tab(it, dataSet[it]!!.root)
            }
        }
    }

    // --- HÀM XUẤT RA EXCEL ---
    private fun exportToCSV() {
        val files = chooseFile("Lưu file CSV", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")), FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files.first()
            // SỬA LỖI Ở ĐÂY: Dùng leftData thay vì items
            val itemsData = itemController.leftData 
            
            // Dùng Charsets.UTF_8 để Excel đọc tiếng Việt không bị lỗi font
            file.printWriter(Charsets.UTF_8).use { out ->
                out.println("ID,Tên (Name),Thuộc tính ẩn (Hex Data),Mô tả (Description)")
                itemsData.forEach { item ->
                    val safeName = item.name.replace(",", " ")
                    val safeDesc = item.description.replace(",", " ")
                    out.println("${item.id},${safeName},${item.extraDataHex},${safeDesc}")
                }
            }
            information("Thành công", "Đã xuất ${itemsData.size} vật phẩm ra file CSV.\nBạn có thể mở bằng Excel để sửa!")
        }
    }

    // --- HÀM NHẬP TỪ EXCEL ---
    private fun importFromCSV() {
        val files = chooseFile("Chọn file CSV đã sửa", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")), FileChooserMode.Single)
        if (files.isNotEmpty()) {
            val file = files.first()
            // SỬA LỖI Ở ĐÂY: Dùng leftData thay vì items
            val itemsData = itemController.leftData 
            var count = 0
            
            file.useLines(Charsets.UTF_8) { lines ->
                val rows = lines.drop(1) // Bỏ qua dòng tiêu đề
                rows.forEach { row ->
                    val columns = row.split(",")
                    if (columns.size >= 4) {
                        val parsedId = columns[0].toIntOrNull()
                        if (parsedId != null) {
                            val targetItem = itemsData.find { it.id == parsedId }
                            if (targetItem != null) {
                                targetItem.name = columns[1]
                                targetItem.extraDataHex = columns[2]
                                targetItem.description = columns[3]
                                count++
                            }
                        }
                    }
                }
            }
            
            // Làm mới giao diện bảng (Buộc UI tải lại dữ liệu mới)
            itemController.observableList.clear()
            itemController.observableList.addAll(itemsData.map { Pair(it, it) })
            
            information("Thành công", "Đã nạp dữ liệu cho $count vật phẩm.\nHãy qua tab Item và bấm [Save data] để lưu file nhé!")
        }
    }
}
