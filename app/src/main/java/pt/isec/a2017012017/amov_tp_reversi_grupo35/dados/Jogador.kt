package pt.isec.a2017012017.amov_tp_reversi_grupo35.dados

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import java.net.Socket

class Jogador (val cor : Int){
    var pontos = 0
    val cor_imagem : Drawable? = null
    var bomba = true
    var troca = true
    var podeJogar = true
    var socket : Socket? = null
    var threadComm : Thread? = null
    var nome : String? = null
    var image : Bitmap? = null

}

