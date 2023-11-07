package pt.isec.a2017012017.amov_tp_reversi_grupo35.dados

class TrocaDados(val cor : Int){
    val sacrificio = mutableListOf<Coordenadas>()
    var recompensa : Coordenadas? = null

    fun sacrificioContem(linha: Int, coluna: Int): Boolean {
        for (c in sacrificio){
            if (c.linha == linha && c.coluna == coluna)
                return true
        }
        return false
    }

    //Retorna a posição das coordenadas introduzidas na lista de sacrificios (-1 se não existirem)
    fun sacrificioPosicao(linha: Int, coluna: Int): Int {
        for (k in 0 until sacrificio.size){
            if (sacrificio[k].linha == linha && sacrificio[k].coluna == coluna)
                return k
        }
        return -1
    }

    fun sacrificioIsFull() : Boolean{
        return sacrificio.size>=2
    }

    fun addSacrificio(linha : Int, coluna: Int){
        sacrificio.add(Coordenadas(linha, coluna))
    }

    fun removeSacrificio(p: Int) {
        sacrificio.removeAt(p)
    }

    fun tratarSacrificio(i: Int, j: Int) : Boolean{
        val p = sacrificioPosicao(i, j)                 //variavel p guarda a posiçao da coordenadas introduzidas na lista de sacrificios
        if ( p == -1 ){                                 //se não existir, e a lista nao estiver cheia, adiciona a nova posição
            if (!sacrificioIsFull()){
                addSacrificio(i,j)
                return true
            }
        }else{                                          //se existir, remove da lista
            removeSacrificio(p)
        }
        return false
    }

    fun anyNull(): Boolean {
        return !sacrificioIsFull() || recompensa == null
    }
}