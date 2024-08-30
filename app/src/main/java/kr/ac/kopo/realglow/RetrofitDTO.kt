package kr.ac.kopo.realglow

import android.icu.text.SelectFormat



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



}