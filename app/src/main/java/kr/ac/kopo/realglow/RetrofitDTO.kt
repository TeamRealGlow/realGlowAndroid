package kr.ac.kopo.realglow

class RetrofitDTO {
    data class hairText
        (
        val Category: String,
        val Itemlen: Int,
        val row: List<Row>)
    {
        data class Row(
            val itemName: String,
            val Color: List<String>,
            val ColorName: List<String>,
            val Company: String,
            val Link: String
        )

    }


    data class skinText
        (
        val Category: String,
        val Itemlen: Int,
        val row: List<Row>)
    {
        data class Row(
            val itemName: String,
            val Color: List<String>,
            val ColorName: List<String>,
            val Company: String,
            val Link: String
        )

    }

    data class lipText
        (
        val Category: String,
        val Itemlen: Int,
        val row: List<Row>)
    {
        data class Row(
            val itemName: String,
            val Color: List<String>,
            val ColorName: List<String>,
            val Company: String,
            val Link: String
        )

    }

    // 이미지 관련 DTO 추가

    data class ParsingRequest(
        val img: String,  // Base64로 인코딩된 이미지 데이터
        val category: String,
        val color: List<Int> // RGB + Alpha 값 (0-255)
    )

    data class MakeupRequest(
        val img: String,
        var parsing: String,
        val skin_color: List<Int>, // RGB + Alpha 값 (0-255)
        val hair_color: List<Int>,
        val lip_color: List<Int>
    )

    data class ParsingResponse(
        val PNGImage: String  // Base64로 인코딩된 변환된 이미지 데이터
    )

    data class MakeupResponse(
        val changeImg: String
    )
}