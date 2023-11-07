package pt.isec.a2017012017.amov_tp_reversi_grupo35.dados

import android.widget.ImageButton

val VAZIO = 0; val BRANCO = 1; val PRETO = 2; val AZUL = 3

class CelulaTabuleiro(val imageButton: ImageButton, val coordenadas: Coordenadas) {
    var cor = VAZIO

}

class Coordenadas(val linha : Int, val coluna: Int) {
}
