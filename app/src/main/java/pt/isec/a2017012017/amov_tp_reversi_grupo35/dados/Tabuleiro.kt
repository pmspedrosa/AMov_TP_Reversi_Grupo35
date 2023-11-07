package pt.isec.a2017012017.amov_tp_reversi_grupo35.dados

import android.util.Log
import java.io.BufferedReader

class Tabuleiro {
    val celulas = mutableListOf<MutableList<CelulaTabuleiro>>()

    fun iniciar2Jogadores() {                //Coloca peças iniciais
        celulas[3][3].cor = BRANCO
        celulas[3][4].cor = PRETO
        celulas[4][3].cor = PRETO
        celulas[4][4].cor = BRANCO
    }

    fun iniciar3Jogadores() {
        celulas[2][4].cor = BRANCO
        celulas[2][5].cor = PRETO
        celulas[3][4].cor = PRETO
        celulas[3][5].cor = BRANCO

        celulas[6][2].cor = AZUL
        celulas[6][3].cor = BRANCO
        celulas[7][2].cor = BRANCO
        celulas[7][3].cor = AZUL

        celulas[6][6].cor = PRETO
        celulas[6][7].cor = AZUL
        celulas[7][6].cor = AZUL
        celulas[7][7].cor = PRETO
    }

    fun isJogadaLegal(i: Int, j: Int, cor: Int): Int {
        if(celulas[i][j].cor == VAZIO){
            if (procurarJogadaLegal(i,j,cor, -1, -1)){
                return 1
            }
            if (procurarJogadaLegal(i,j,cor, -1, 0)){
                return 2
            }
            if (procurarJogadaLegal(i,j,cor, -1, 1)){
                return 3
            }
            if (procurarJogadaLegal(i,j,cor, 0, -1)){
                return 4
            }
            if (procurarJogadaLegal(i,j,cor, 0, 1)){
                return 5
            }
            if (procurarJogadaLegal(i,j,cor, 1, -1)){
                return 6
            }
            if (procurarJogadaLegal(i,j,cor, 1, 0)){
                return 7
            }
            if (procurarJogadaLegal(i,j,cor, 1, 1)){
                return 8
            }
        }
        return 0;
    }

    private fun procurarJogadaLegal(i: Int, j: Int, cor: Int,verticalInc : Int, horizontalInc : Int): Boolean {
        var temAdversario = false
        var linha = i+verticalInc; var coluna = j+horizontalInc     //procura celula na direção indicada pelos incrementos
        while (linha>=0 && coluna>=0 && linha<celulas.size && coluna<celulas.size){
            if (celulas[linha][coluna].cor == VAZIO){       //se celula for vazia, retorna false
                return false
            }else if (celulas[linha][coluna].cor != cor){   //se celula for do adversario, pode ser direção viável
                temAdversario = true
            }else {
                return celulas[linha][coluna].cor == cor && temAdversario     //se celula for do proprio jogador, e tiver encontrado uma celula do adversario, retorna true, senão retorna false
            }
            linha += verticalInc; coluna+=horizontalInc
        }

    return false
    }

    fun getTam(): Int {
        return celulas.size
    }

    fun fazJogada(i: Int, j: Int, cor: Int, direcao: Int?) : Boolean {
        if (direcao == null) {
            return false
        }
        val listaVirar = mutableListOf<Int>(direcao)
        listaVirar.addAll(procuraTodasJogadasLegais(i, j, cor, direcao))
        for (d in listaVirar)
            Log.i("TAG", "fazJogada: $d")
        //virar
        jogadaVirarPecas(i, j, cor, listaVirar)

        return true
    }

    private fun jogadaVirarPecas(i: Int, j: Int, cor: Int, listaVirar: MutableList<Int>) {
        celulas[i][j].cor = cor
        for (dir in listaVirar){
            when(dir){
                1->{
                    virarPecas(i, j, cor, -1, -1)
                }
                2->{
                    virarPecas(i, j, cor, -1, 0)
                }
                3->{
                    virarPecas(i, j, cor, -1, 1)
                }
                4->{
                    virarPecas(i, j, cor, 0, -1)
                }
                5->{
                    virarPecas(i, j, cor, 0, 1)
                }
                6->{
                    virarPecas(i, j, cor, 1, -1)
                }
                7->{
                    virarPecas(i, j, cor, 1, 0)
                }
                8->{
                    virarPecas(i, j, cor, 1, 1)
                }
            }
        }
    }

    private fun virarPecas(i: Int, j: Int, cor: Int, verticalInc: Int, horizontalInc: Int) {
        var linha = i+verticalInc; var coluna = j+horizontalInc
        while (linha>=0 && coluna>=0 && linha<celulas.size && coluna<celulas.size){
            if (celulas[linha][coluna].cor != cor){         //se celula for do adversario, trocar cor
                    celulas[linha][coluna].cor = cor
            }else
                break                                       //se celula for do proprio jogador, ou vazia, sair
            linha += verticalInc; coluna+=horizontalInc
        }
    }

    private fun procuraTodasJogadasLegais(i: Int, j: Int, cor: Int, direcao: Int): MutableList<Int> {
        val lista = mutableListOf<Int>()
        var direcaoInc = direcao
        while (direcaoInc<8){
            when(direcaoInc){
                1->{ if (procurarJogadaLegal(i, j, cor, -1, 0))
                    lista.add(direcaoInc+1) }
                2->{ if (procurarJogadaLegal(i, j, cor, -1, 1))
                    lista.add(direcaoInc+1) }
                3->{ if (procurarJogadaLegal(i, j, cor, 0, -1))
                    lista.add(direcaoInc+1) }
                4->{ if (procurarJogadaLegal(i, j, cor, 0, 1))
                    lista.add(direcaoInc+1) }
                5->{ if (procurarJogadaLegal(i, j, cor, 1, -1))
                    lista.add(direcaoInc+1) }
                6->{ if (procurarJogadaLegal(i, j, cor, 1, 0))
                    lista.add(direcaoInc+1) }
                7->{ if (procurarJogadaLegal(i, j, cor, 1, 1))
                    lista.add(direcaoInc+1) }
            }
            direcaoInc++
        }
        return lista
    }

    fun isFull(): Boolean {
        for (l in celulas)
            for (c in l)
                if (c.cor == VAZIO)
                    return false
        Log.i("TAG", "isFull: true")
        return true
    }

    fun fazBomba(i: Int, j: Int, cor: Int) : Boolean{
        if (celulas[i][j].cor != cor)
            return false
        apagaPeca(i-1, j-1)
        apagaPeca(i-1, j)
        apagaPeca(i-1, j+1)
        apagaPeca(i, j-1)
        apagaPeca(i, j+1)
        apagaPeca(i+1, j-1)
        apagaPeca(i+1, j)
        apagaPeca(i+1, j+1)
        return true
    }

    private fun apagaPeca(linha: Int, coluna: Int) {
        if (linha >= celulas.size || linha < 0 || coluna >= celulas.size || coluna < 0)
            return
        celulas[linha][coluna].cor = VAZIO
    }

    fun getCor(coordenadas: Coordenadas) : Int{
        return getCelula(coordenadas).cor
    }

    fun getCelula(c: Coordenadas) : CelulaTabuleiro{
        return celulas[c.linha][c.coluna]
    }

    fun fazTroca(trocaDados: TrocaDados): Boolean {
        if (!trocaDados.anyNull()) {
            for (c in trocaDados.sacrificio) {
                celulas[c.linha][c.coluna].cor = getCor(trocaDados.recompensa!!)
            }
            celulas[trocaDados.recompensa!!.linha][trocaDados.recompensa!!.coluna].cor = trocaDados.cor
            return true
        }
        return false
    }
}