package vn.jupiter.tsdata.data

import vn.jupiter.tsdata.controller.isNumber
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.regex.Pattern
import javax.xml.stream.events.Characters
import kotlin.experimental.and

sealed class TSModel(val byteData: ByteBuffer, val itemSize: Int, val charset: Charset = Charset.forName("Big5")) {
    open var id: Int = -1
    open var name: String = ""
        set(value) {
            if (!value.contentEquals(field)) {
                field = value.substring(0, minOf(value.length, nameLength))
                val stringLength = saveString(field, nameIdx, nameLength)
                saveByte(stringLength, nameSizeIdx)
            }
        }
    open var description: String = ""
        set(value) {
            if (!value.contentEquals(field) && descSizeIdx > -1) {
                field = value.substring(0, minOf(value.length, descLength))
                val stringLength = saveString(value, descIdx, descLength)
                saveByte(stringLength, descSizeIdx)
            }
        }

    open var nameIdx: Int = 0
    open var nameLength: Int = 0
    open var nameSizeIdx = 0

    open var descIdx: Int = -1
    open var descSizeIdx = -1
    open var descLength: Int = -1

    companion object {
        val VISCII = arrayOf(128, 132, 192, 193, 194, 195, 196, 197, 141, 142, 200, 201, 202, 203, 204, 205, 206, 144, 145, 179, 180, 210, 211, 212, 157, 158, 185, 186, 187, 188, 217, 218, 159, 161, 162, 163, 164, 165, 166, 167, 198, 199, 213, 224, 225, 226, 227, 228, 229, 230, 231, 168, 169, 170, 171, 172, 173, 174, 232, 233, 234, 235, 184, 236, 237, 238, 239, 175, 176, 177, 178, 181, 182, 183, 189, 190, 222, 242, 243, 244, 245, 246, 247, 254, 209, 215, 216, 223, 241, 248, 249, 250, 251, 252, 255, 207, 214, 219, 220, 253, 208, 240)
        val ASCII = arrayOf(65, 65, 65, 65, 65, 65, 65, 65, 69, 69, 69, 69, 69, 69, 73, 73, 73, 79, 79, 79, 79, 79, 79, 79, 85, 85, 85, 85, 85, 85, 85, 85, 89, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 117, 97, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 105, 105, 105, 105, 105, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 121, 121, 121, 121, 121, 68, 100)
    }

    init {
    }

    // HÀM LƯU ID ĐƯỢC CHỈNH SỬA
    open fun saveId(newId: Int) {
    }

    fun saveShort(value: Int, start: Int) {
        byteData.put(start, (value and 0xFF).toByte())
        byteData.put(start + 1, ((value ushr 8) and 0xFF).toByte())
    }

    fun readString(start: Int, lengthInBytes: Int, isReverse: Boolean = true): String {
        val stringBytes = ByteArray(lengthInBytes)
        byteData.position(start)
        byteData.get(stringBytes, 0, lengthInBytes)
        if (isReverse) {
            stringBytes.reverse()
        }
        return charset.decode(ByteBuffer.wrap(stringBytes)).toString()
    }

    fun readByte(start: Int): Short {
        return byteData.get(start).toPositiveInt()
    }

    fun readShort(start: Int): Int {
        return readNumber(start, 2).toInt()
    }

    fun readLong(start: Int): Long {
        return readNumber(start, 4)
    }

    private fun readNumber(start: Int, noOfBytes: Int): Long {
        var result: Long = 0
        for (idx in (noOfBytes - 1) downTo 0) {
            result = result * 256 + byteData.get(start + idx).toPositiveInt()
        }
        return result
    }

    fun saveString(text: String, start: Int, targetLength: Int): Int {
        val encodedBytes = charset.encode(text)
        val encodedLength = encodedBytes.limit()
        val emptyBytesCount = maxOf(0, targetLength - encodedLength)
        fillEmpty(start, emptyBytesCount)
        byteData.position(start + emptyBytesCount)
        val destinationByteCount = minOf(targetLength, encodedLength)
        val stringArray = ByteArray(destinationByteCount)
        encodedBytes.position(0)
        encodedBytes.get(stringArray, 0, destinationByteCount)
        byteData.put(stringArray.reversedArray(), 0, destinationByteCount)
        return destinationByteCount
    }

    fun saveStringNoRev(text: String, start: Int, targetLength: Int): Int {
        val encodedBytes = charset.encode(text)
        val encodedLength = encodedBytes.limit()
        val emptyBytesCount = maxOf(0, targetLength - encodedLength)
        val destinationByteCount = minOf(targetLength, encodedLength)
        val stringArray = ByteArray(destinationByteCount)
        encodedBytes.position(0)
        encodedBytes.get(stringArray, 0, destinationByteCount)
        byteData.position(start)
        byteData.put(stringArray, 0, destinationByteCount)
        fillEmpty(start + destinationByteCount, emptyBytesCount)
        return destinationByteCount
    }

    protected fun convertToKD(start: Int, lengthInBytes: Int, isReverse: Boolean = true): String {
        val result = StringBuilder()
        byteData.position(start)
        for (i in (lengthInBytes - 1) downTo 0) {
            val stringByte = readByte(start + i)
            val visciiId = VISCII.indexOf(stringByte.toInt())
            if (visciiId > -1) {
                result.append(ASCII[visciiId].toChar())
            } else if (stringByte != 0x0.toShort()) {
                result.append(stringByte.toChar())
            }
        }
        if (!isReverse) {
            result.reverse()
        }
        return result.toString()
    }

    open fun getNameKD(): String = convertToKD(nameIdx, nameLength)

    fun getDesKD() = convertToKD(descIdx, descLength)

    fun saveByte(value: Int, start: Int) {
        byteData.put(start, (value and 0xFF).toByte())
    }

    fun fillEmpty(start: Int, length: Int) {
        if (length > 0) {
            for (idx in 0..(length - 1)) {
                byteData.position(start + idx)
                byteData.put(0x0)
            }
        }
    }

    fun toHexString(): String {
        return byteData.array().toHex()
    }

    fun getHexString(start: Int, lengthInBytes: Int): String {
        val tempArray = ByteArray(lengthInBytes)
        val oldPosition = byteData.position()
        byteData.position(start)
        byteData.get(tempArray, 0, lengthInBytes)
        byteData.position(oldPosition)
        return tempArray.toHex()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TSModel
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id
    }

    fun hasStrangeName(): Boolean {
        return name.isNumber() || name.hasChineseCharacter()
    }
}

fun String.hasChineseCharacter(): Boolean {
    forEach {
        if (Character.isIdeographic(it.toInt())) {
            return true
        }
    }
    return false
}

class Item(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    companion object {
        val NAME_SIZE_INDEX = 0
        val NAME_INDEX = 1
        val NAME_LENGTH = 20
        val ID_INDEX = 22
        val DESCRIPTION_SIZE_INDEX = 115
        val DESCRIPTION_INDEX = 116
        val DESCRIPTION_LENGTH = 254
    }

    init {
        nameIdx = NAME_INDEX
        nameSizeIdx = NAME_SIZE_INDEX
        nameLength = NAME_LENGTH
        descIdx = DESCRIPTION_INDEX
        descSizeIdx = DESCRIPTION_SIZE_INDEX
        descLength = DESCRIPTION_LENGTH

        id = decode16(ID_INDEX)
        val nameSize = readByte(nameSizeIdx).toInt()
        name = readString(nameIdx + maxOf(nameLength - nameSize, 0), nameSize)
        val descriptionSize = readByte(descSizeIdx).toInt()
        description = readString(descIdx + descLength - descriptionSize, descriptionSize)
    }

    // --- BỘ CÔNG CỤ MÃ HÓA / GIẢI MÃ (Dịch từ C# của bạn) ---
    fun saveLong(value: Long, start: Int) {
        byteData.put(start, (value and 0xFF).toByte())
        byteData.put(start + 1, ((value ushr 8) and 0xFF).toByte())
        byteData.put(start + 2, ((value ushr 16) and 0xFF).toByte())
        byteData.put(start + 3, ((value ushr 24) and 0xFF).toByte())
    }

    private fun decode8(index: Int): Int = ((readByte(index).toInt() xor 154) - 9) and 0xFF
    private fun encode8(index: Int, value: Int) = saveByte(((value + 9) xor 154) and 0xFF, index)

    private fun decode16(index: Int): Int = ((readShort(index) xor 61379) - 9) and 0xFFFF
    private fun encode16(index: Int, value: Int) = saveShort(((value + 9) xor 61379) and 0xFFFF, index)

    private fun decode32(index: Int): Long = ((readLong(index) xor 193000628L) - 9L) and 0xFFFFFFFFL
    private fun encode32(index: Int, value: Long) = saveLong(((value + 9L) xor 193000628L) and 0xFFFFFFFFL, index)

    private fun decode32s(index: Int): Int = (readLong(index).toInt() xor 193000628) - 109
    private fun encode32s(index: Int, value: Int) = saveLong(((value + 109) xor 193000628).toLong(), index)

    override fun saveId(newId: Int) {
        this.id = newId
        encode16(ID_INDEX, newId)
    }

    // --- BẢN ĐỒ GIẢI PHẪU 30 CỘT DỮ LIỆU ---
    var type: Int get() = decode8(21); set(v) = encode8(21, v)
    var picId: Int get() = decode16(24); set(v) = encode16(24, v)
    var largeIconNum: Int get() = decode16(26); set(v) = encode16(26, v)
    var equipImage1: Int get() = decode16(28); set(v) = encode16(28, v)
    var equipImage2: Int get() = decode16(30); set(v) = encode16(30, v)
    var prop1: Int get() = decode16(32); set(v) = encode16(32, v)
    var prop2: Int get() = decode16(34); set(v) = encode16(34, v)
    var unk1: Int get() = decode8(36); set(v) = encode8(36, v)
    var unk2: Int get() = decode8(37); set(v) = encode8(37, v)
    var prop1Val: Int get() = decode32s(38); set(v) = encode32s(38, v)
    var prop2Val: Int get() = decode32s(42); set(v) = encode32s(42, v)
    var contribute: Int get() = decode8(46); set(v) = encode8(46, v)
    var sellPrice1: Int get() = decode8(47); set(v) = encode8(47, v)
    var equipPos: Int get() = decode8(48); set(v) = encode8(48, v)
    var unk3: Int get() = decode8(49); set(v) = encode8(49, v)
    
    var colorDefHex: String 
        get() = getHexString(50, 32) // Màu sắc không cần sửa tay, lưu dưới dạng Hex cho an toàn
        set(v) {
            val clean = v.replace(" ", "").trim()
            if (clean.length == 64) {
                val oldPos = byteData.position()
                byteData.position(50)
                for (i in 0 until 32) {
                    byteData.put(clean.substring(i*2, i*2+2).toInt(16).toByte())
                }
                byteData.position(oldPos)
            }
        }
        
    var unk4: Int get() = decode8(82); set(v) = encode8(82, v)
    var level: Int get() = decode8(83); set(v) = encode8(83, v)
    var buyingPrice: Long get() = decode32(84); set(v) = encode32(84, v)
    var sellingPrice: Long get() = decode32(88); set(v) = encode32(88, v)
    var equipLimit: Int get() = decode8(92); set(v) = encode8(92, v)
    var unk5: Int get() = decode8(93); set(v) = encode8(93, v)
    var unk6: Long get() = decode32(94); set(v) = encode32(94, v)
    var elemType: Int get() = decode8(98); set(v) = encode8(98, v)
    var elemVal: Int get() = decode32s(99); set(v) = encode32s(99, v)
    var unk9: Int get() = decode16(103); set(v) = encode16(103, v)
    var unk10: Int get() = decode8(105); set(v) = encode8(105, v)
    var unk11: Int get() = decode16(106); set(v) = encode16(106, v)
    var unk12: Int get() = decode8(108); set(v) = encode8(108, v)
    var unk13: Int get() = decode16(109); set(v) = encode16(109, v)
    var unk14: Int get() = decode8(111); set(v) = encode8(111, v)
    var unk15: Int get() = decode16(112); set(v) = encode16(112, v)
    var unk16: Int get() = decode8(114); set(v) = encode8(114, v)

    override fun toString(): String {
        return "Item id: $id name: $name desc: $description"
    }
}

class NPC(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    val idIndex: Int

    companion object {
        val NAME_SIZE_INDEX = 0
        val NAME_INDEX = 1
    }

    init {
        nameIdx = NAME_INDEX
        when (itemSize) {
            88 -> {
                nameLength = 10
            }
            92 -> {
                nameLength = 14
            }
            else -> {
                nameLength = 10
            }
        }
        nameSizeIdx = NAME_SIZE_INDEX

        idIndex = NAME_INDEX + nameLength + 1
        id = readShort(idIndex).xor(0x5209) - 1
        var nameSize = readByte(nameSizeIdx)
        nameSize = minOf(nameSize, nameLength.toShort())
        name = readString(nameIdx + nameLength - nameSize, nameSize.toInt())
    }

    override fun toString(): String {
        return "Item id: $id name: $name"
    }
}

class Talk(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    companion object {
        val NAME_SIZE_INDEX = 2
        val NAME_INDEX = 3
    }

    init {
        nameIdx = NAME_INDEX
        nameLength = itemSize - 3
        nameSizeIdx = NAME_SIZE_INDEX

        id = readShort(0).xor(0xEC88).xor(0x62) - 5
        var nameSize = readByte(nameSizeIdx)
        nameSize = minOf(nameSize, nameLength.toShort())
        name = readString(NAME_INDEX + nameLength - nameSize, nameSize.toInt())
    }
}

class Skill(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    companion object {
        val NAME_SIZE_INDEX = 0
        val NAME_INDEX = 1
    }

    init {
        nameIdx = NAME_INDEX
        nameLength = 20
        nameSizeIdx = NAME_SIZE_INDEX

        id = readShort(nameIdx + nameLength + 1).xor(0x6EA0) - 4
        var nameSize = readByte(nameSizeIdx)
        nameSize = minOf(nameSize, nameLength.toShort())
        name = readString(nameIdx + nameLength - nameSize, nameSize.toInt())
    }
}

class Scene(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    override var name: String = ""
        set(value) {
            if (!value.contentEquals(field)) {
                field = value.substring(0, minOf(value.length, nameLength))
                val stringLength = saveStringNoRev(field, nameIdx, nameLength)
                saveByte(stringLength, nameSizeIdx)
            }
        }

    init {
        nameIdx = 4
        nameLength = 20
        nameSizeIdx = 3

        id = readShort(0).xor(0xEA6F).xor(0x3) - 9
        var nameSize = readByte(nameSizeIdx)
        nameSize = minOf(nameSize, nameLength.toShort())
        name = readString(nameIdx, nameSize.toInt(), isReverse = false)
    }

    override fun getNameKD(): String = convertToKD(nameIdx, nameLength, false)
}

class ShopItem(byteData: ByteBuffer, itemSize: Int, charset: Charset = Charset.forName("Big5")) : TSModel(byteData, itemSize, charset) {
    
    // Giả định cấu trúc cơ bản của file8.dat: [2 byte ShopID] [1 byte Index] [2 byte ItemID]
    // Nếu đọc lên Tool bị ra số rác, bro chỉ cần thêm .xor(0xMã_Hex) vào sau hàm read/save giống như các file khác nhé!
    var shopIdVal: Int
        get() = readShort(0) 
        set(value) = saveShort(value, 0)

    var itemIndexVal: Int
        get() = readByte(2).toInt()
        set(value) = saveByte(value, 2)

    var itemIdVal: Int
        get() = readShort(3)
        set(value) = saveShort(value, 3)

    init {
        id = shopIdVal
    }

    override fun saveId(newId: Int) {
        shopIdVal = newId
        id = newId
    }

    // Cú lừa UI số 1: Biến cột "Name" thành Item ID
    override var name: String
        // Thêm khoảng trắng vào cuối để lừa hàm isNumber() của Tool, giúp ô này không bị bôi màu đỏ do trùng định dạng số
        get() = itemIdVal.toString() + " " 
        set(value) {
            val parsed = value.trim().toIntOrNull() ?: 0
            itemIdVal = parsed
        }

    // Cú lừa UI số 2: Biến cột "Description" thành Index (Vị trí ô đồ)
    override var description: String
        get() = itemIndexVal.toString() + " "
        set(value) {
            val parsed = value.trim().toIntOrNull() ?: 0
            itemIndexVal = parsed
        }
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

fun Byte.toPositiveInt() = toShort() and 0xFF

fun ByteArray.toHex(): String {
    val result = StringBuffer()

    forEachIndexed { index, byte ->
        val octet = byte.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}
