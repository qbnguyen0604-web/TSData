package vn.jupiter.tsdata.view

import javafx.stage.FileChooser
import tornadofx.*
import vn.jupiter.tsdata.controller.*
import vn.jupiter.tsdata.data.Item

class MainView : View("TS Data Editor - Ultimate Version") {
    
    val itemRepo = ItemInfoDataRepo()
    val itemController = ItemTabController(itemRepo)
    
    val dataSet = mapOf(
            "Item" to ItemTabView(itemController),
            "NPC" to ItemTabView(ItemTabController(NpcInfoDataRepo(), NpcInfoDataRepo())),
            "Talk" to ItemTabView(ItemTabController(TalkDataRepo())),
            "Skill" to ItemTabView(ItemTabController(SkillDataRepo())),
            "Scene" to ItemTabView(ItemTabController(SceneSkillDataRepo()))
            "Shop (file8.dat)" to ItemTabView(ItemTabController(ShopDataRepo(), ShopDataRepo()))
    )

    override val root = borderpane {
        top = menubar {
            menu("Xử lý Hàng Loạt (Excel/CSV)") {
                item("1. Xuất Item.Dat ra CSV (Toàn bộ 30 Cột)").action { exportToCSV() }
                item("2. Nhập CSV vào Tool").action { importFromCSV() }
            }
        }
        center = tabpane {
            dataSet.keys.forEach { tab(it, dataSet[it]!!.root) }
        }
    }

    private fun exportToCSV() {
        val files = chooseFile("Lưu file CSV", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")), FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files.first()
            val itemsData = itemController.leftData 
            
            file.printWriter(Charsets.UTF_8).use { out ->
                out.println("ID,Name,Type,PicID,LargePicID,EquipImg1,EquipImg2,Prop1,Prop1Val,Prop2,Prop2Val,ElemType,ElemVal,Contribute,SellPrice1,EquipPos,Level,BuyPrice,SellPrice2,EquipLimit,ColorDefHex,Unk1,Unk2,Unk3,Unk4,Unk5,Unk6,Unk9,Unk10,Unk11,Unk12,Unk13,Unk14,Unk15,Unk16,Description")
                itemsData.forEach { i ->
                    // Khử cả dấu phẩy và chấm phẩy trong văn bản để chống lỗi Excel
                    val sName = i.name.replace(",", " ").replace(";", " ")
                    val sDesc = i.description.replace(",", " ").replace(";", " ")
                    out.println("${i.id},${sName},${i.type},${i.picId},${i.largeIconNum},${i.equipImage1},${i.equipImage2},${i.prop1},${i.prop1Val},${i.prop2},${i.prop2Val},${i.elemType},${i.elemVal},${i.contribute},${i.sellPrice1},${i.equipPos},${i.level},${i.buyingPrice},${i.sellingPrice},${i.equipLimit},${i.colorDefHex},${i.unk1},${i.unk2},${i.unk3},${i.unk4},${i.unk5},${i.unk6},${i.unk9},${i.unk10},${i.unk11},${i.unk12},${i.unk13},${i.unk14},${i.unk15},${i.unk16},${sDesc}")
                }
            }
            information("Thành công", "Đã xuất Full 30 cột ra CSV.\nĐừng quên dùng File -> Save As -> CSV UTF-8 khi lưu trên Excel nhé!")
        }
    }

    private fun importFromCSV() {
        val files = chooseFile("Chọn file CSV đã sửa", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")), FileChooserMode.Single)
        if (files.isNotEmpty()) {
            val file = files.first()
            val itemsData = itemController.leftData 
            var count = 0
            
            file.useLines(Charsets.UTF_8) { lines ->
                val rows = lines.drop(1) 
                rows.forEach { row ->
                    // Tính năng tự động phát hiện Excel dùng dấu phẩy hay chấm phẩy
                    val separator = if (row.contains(";")) ";" else ","
                    // Cắt các cột ra và ép nó thành danh sách có thể thay đổi
                    val cols = row.split(separator).toMutableList()
                    // Nếu cột Mô tả bị rỗng làm hụt size, tự động đắp thêm khoảng trống vào cho đủ 36 cột
                    while (cols.size < 36) cols.add("")
                    
                    if (cols.size >= 36) { 
                        val parsedId = cols[0].toIntOrNull()
                        if (parsedId != null) {
                            val t = itemsData.find { it.id == parsedId }
                            if (t != null) {
                                t.name = cols[1]; t.type = cols[2].toInt(); t.picId = cols[3].toInt()
                                t.largeIconNum = cols[4].toInt(); t.equipImage1 = cols[5].toInt()
                                t.equipImage2 = cols[6].toInt(); t.prop1 = cols[7].toInt()
                                t.prop1Val = cols[8].toInt(); t.prop2 = cols[9].toInt()
                                t.prop2Val = cols[10].toInt(); t.elemType = cols[11].toInt()
                                t.elemVal = cols[12].toInt(); t.contribute = cols[13].toInt()
                                t.sellPrice1 = cols[14].toInt(); t.equipPos = cols[15].toInt()
                                t.level = cols[16].toInt(); t.buyingPrice = cols[17].toLong()
                                t.sellingPrice = cols[18].toLong(); t.equipLimit = cols[19].toInt()
                                t.colorDefHex = cols[20]; t.unk1 = cols[21].toInt()
                                t.unk2 = cols[22].toInt(); t.unk3 = cols[23].toInt()
                                t.unk4 = cols[24].toInt(); t.unk5 = cols[25].toInt()
                                t.unk6 = cols[26].toLong(); t.unk9 = cols[27].toInt()
                                t.unk10 = cols[28].toInt(); t.unk11 = cols[29].toInt()
                                t.unk12 = cols[30].toInt(); t.unk13 = cols[31].toInt()
                                t.unk14 = cols[32].toInt(); t.unk15 = cols[33].toInt()
                                t.unk16 = cols[34].toInt(); t.description = cols[35]
                                count++
                            }
                        }
                    }
                }
            }
            itemController.observableList.clear()
            itemController.observableList.addAll(itemsData.map { Pair(it, it) })
            information("Thành công", "Đã nạp $count vật phẩm. Hãy bấm [Save data] để lưu vào Item.Dat!")
        }
    }
}
