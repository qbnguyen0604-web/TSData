package vn.jupiter.tsdata.view

import javafx.stage.FileChooser
import tornadofx.*
import vn.jupiter.tsdata.controller.*
import vn.jupiter.tsdata.data.Item
import java.io.File

class MainView : View("TS Data Editor - Ultimate Version") {
    
    val itemRepo = ItemInfoDataRepo()
    val itemController = ItemTabController(itemRepo)
    
    val dataSet = mapOf(
            "Item" to ItemTabView(itemController),
            "NPC" to ItemTabView(ItemTabController(NpcInfoDataRepo(), NpcInfoDataRepo())),
            "Talk" to ItemTabView(ItemTabController(TalkDataRepo())),
            "Skill" to ItemTabView(ItemTabController(SkillDataRepo())),
            "Scene" to ItemTabView(ItemTabController(SceneSkillDataRepo()))
    )

    // Thiết kế lại Giao diện: Thêm Thanh Công Cụ (Menu Bar) ở phía trên
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
            val items = itemController.items // Lấy danh sách item đang load
            
            file.printWriter().use { out ->
                // Tạo tiêu đề cột cho Excel
                out.println("ID,Tên (Name),Thuộc tính ẩn (Hex Data),Mô tả (Description)")
                items.forEach { item ->
                    val safeName = item.name.replace(",", " ")
                    val safeDesc = item.description.replace(",", " ")
                    out.println("${item.id},${safeName},${item.extraDataHex},${safeDesc}")
                }
            }
            information("Thành công", "Đã xuất ${items.size} vật phẩm ra file CSV.\nBạn có thể mở bằng Excel để sửa!")
        }
    }

    // --- HÀM NHẬP TỪ EXCEL ---
    private fun importFromCSV() {
        val files = chooseFile("Chọn file CSV đã sửa", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")), FileChooserMode.Single)
        if (files.isNotEmpty()) {
            val file = files.first()
            val items = itemController.items
            var count = 0
            
            file.useLines { lines ->
                val rows = lines.drop(1) // Bỏ qua dòng tiêu đề
                rows.forEach { row ->
                    val columns = row.split(",")
                    if (columns.size >= 4) {
                        val parsedId = columns[0].toIntOrNull()
                        if (parsedId != null) {
                            // Tìm item tương ứng trong Tool và ghi đè dữ liệu mới
                            val targetItem = items.find { it.id == parsedId } as? Item
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
            // Làm mới bảng hiển thị
            itemController.items.setAll(items)
            information("Thành công", "Đã nạp dữ liệu cho $count vật phẩm.\nHãy qua tab Item và bấm [Save data] để lưu lại vào file Item.Dat nhé!")
        }
    }
}
