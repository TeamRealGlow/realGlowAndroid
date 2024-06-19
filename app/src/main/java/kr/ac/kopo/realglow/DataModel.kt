package kr.ac.kopo.realglow

data class DataModel(
    val Category: String,
    val Itemlen: Int,
    val row: List<Row>
) {
    data class Row(
        val ItemName: ItemInfo,
        val Color: ItemInfo,
        val Company: ItemInfo,
        val Link: ItemInfo
    )

    data class ItemInfo(val a: String)
}