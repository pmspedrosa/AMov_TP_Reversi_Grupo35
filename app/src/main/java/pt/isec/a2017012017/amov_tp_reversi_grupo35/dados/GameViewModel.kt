package pt.isec.a2017012017.amov_tp_reversi_grupo35.dados

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import pt.isec.a2017012017.amov_tp_reversi_grupo35.*
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random

import android.graphics.BitmapFactory
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_perfil.*
import org.json.JSONArray
import java.io.File
import java.io.PrintStream
import kotlin.coroutines.coroutineContext


const val SERVER_PORT = 9999
const val TAG = "tag"


class GameViewModel : ViewModel() {
    enum class State {
        STARTING, TURN_ENDED, GAME_ENDED, PROCESSING_MOVE,
        LOCAL_PLAYING_REGULAR, LOCAL_PLAYING_BOMB, LOCAL_PLAYING_SWAP, LOCAL_UNPLAYABLE,
        CLIENT_PLAYING_REGULAR, CLIENT_PLAYING_BOMB, CLIENT_PLAYING_SWAP, CLIENT_UNPLAYABLE
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, GETTING_INFO, CONNECTION_ESTABLISHED, HALF_CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED
    }

    var state = MutableLiveData(State.STARTING)
    var connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    var modoJogo = -1
    var isServer = false
    val tabuleiro = Tabuleiro()
    val jogadores = mutableListOf<Jogador>()
    var jogadorAtual = 1
    val jogadasLegais = mutableListOf<JogadaLegal>()
    var dicas = true        //Deverá ser alterado com um botão
    var readyForClick = false
    var trocaDados = TrocaDados(-1)
    var jogadas = 0
    val db = Firebase.firestore
    var numConnected = 0
    private var jogadasEspeciais = false
    private var nomeClienteTemp : String? = null
    var defaultImage : Bitmap? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientThreadComm: Thread? = null




    fun trataClique(i: Int, j: Int) {
        if (modoJogo == 1)
            trataCliqueServidor(i, j)
        else if (isServer)
            trataCliqueServidor(i, j)
        else
            trataCliqueCliente(i, j)
    }

    private fun trataCliqueCliente(linha: Int, coluna: Int) {
        Log.i(TAG, "trataCliqueCliente: trata Clique cliente!")
        if(clientSocket?.getOutputStream() == null){
            return
        }

        Log.i(TAG, "trataCliqueCliente: socket nao null")

        if (state.value == State.LOCAL_PLAYING_REGULAR || state.value == State.LOCAL_PLAYING_BOMB || state.value == State.LOCAL_PLAYING_SWAP){

            when (state.value){
                State.LOCAL_PLAYING_REGULAR, State.LOCAL_PLAYING_BOMB -> {
                    val jsonObject = JSONObject()

                    jsonObject.put("tipo", "clique")
                    jsonObject.put("linha", linha)
                    jsonObject.put("coluna", coluna)
                    jsonObject.put("powerup", if (state.value == State.LOCAL_PLAYING_REGULAR) "none" else "bomb")

                    thread {
                        enviaJsonObject(clientSocket, jsonObject)
                    }
                    state.value = State.PROCESSING_MOVE
                }
                State.LOCAL_PLAYING_SWAP -> {
                    if (jogadores[jogadorAtual].troca) {
                        state.value = State.PROCESSING_MOVE

                        if (tabuleiro.celulas[linha][coluna].cor == jogadores[jogadorAtual].cor) {
                            trocaDados!!.tratarSacrificio(linha, coluna)
                            state.value = State.LOCAL_PLAYING_SWAP
                        } else if (tabuleiro.celulas[linha][coluna].cor != VAZIO) {
                            if (trocaDados!!.sacrificioIsFull()) {
                                trocaDados!!.recompensa = Coordenadas(linha, coluna)

                                val jsonObject = JSONObject()

                                jsonObject.put("tipo", "clique")
                                val trocaArray = JSONArray()
                                for (i in 0 until trocaDados!!.sacrificio.size) {
                                    val jsonTroca = JSONObject()
                                    jsonTroca.put("linha", trocaDados.sacrificio[i].linha)
                                    jsonTroca.put("coluna", trocaDados.sacrificio[i].coluna)
                                    trocaArray.put(jsonTroca)
                                }
                                jsonObject.put("sacrificio", trocaArray)
                                jsonObject.put("linha", linha)
                                jsonObject.put("coluna", coluna)
                                jsonObject.put("powerup", "swap")

                                thread {
                                    enviaJsonObject(clientSocket, jsonObject)
                                }
                                trocaDados.sacrificio.clear()
                                state.value = State.PROCESSING_MOVE
                            }else{
                                state.value = State.LOCAL_PLAYING_SWAP
                            }
                        }else {
                            state.value = State.LOCAL_PLAYING_SWAP
                        }
                    }
                }
            }
        }
    }

    fun trataCliqueServidor(i: Int, j: Int) {
        val oldState = state.value
        if (state.value == State.LOCAL_PLAYING_REGULAR) {
            state.value = State.PROCESSING_MOVE
            if (tabuleiro.fazJogada(
                    i,
                    j,
                    jogadores[jogadorAtual].cor,
                    jogadasLegais.find { jogadaLegal -> jogadaLegal.linha == i && jogadaLegal.coluna == j }?.direcao
                )
            ) {
                //state.value = State.TURN_ENDED
                terminaJogada()
            } else
                state.value = oldState
        } else if (state.value == State.LOCAL_PLAYING_BOMB && jogadores[jogadorAtual].bomba) {
            state.value = State.PROCESSING_MOVE

            if (tabuleiro.fazBomba(i, j, jogadores[jogadorAtual].cor)) {
                jogadores[jogadorAtual].bomba = false
                terminaJogada()
            } else {
                state.value = oldState
            }
        } else if (state.value == State.LOCAL_PLAYING_SWAP && jogadores[jogadorAtual].troca) {
            state.value = State.PROCESSING_MOVE

            if (tabuleiro.celulas[i][j].cor == jogadores[jogadorAtual].cor) {
                trocaDados!!.tratarSacrificio(i, j)
                state.value = oldState
            } else if (tabuleiro.celulas[i][j].cor != VAZIO) {
                if (trocaDados!!.sacrificioIsFull()) {
                    trocaDados!!.recompensa = Coordenadas(i, j)
                    if (tabuleiro.fazTroca(trocaDados!!)) {
                        jogadores[jogadorAtual].troca = false
                        terminaJogada()
                    } else {
                        state.value = oldState
                    }
                }else{
                    state.value = oldState
                }
            }else{
                state.value = oldState
            }
        }
        return
    }

    private fun terminaJogada() {
        jogadas++

        if (tabuleiro.isFull() || ninguemPodeJogar()) {
            terminaJogo()
            enviaTabuleiroAosClientes()
            enviaFimDeJogoAosClientes()

        } else {
            proximoJogador()
            trocaDados = TrocaDados(getCorAtual())
            updatePontuacao()
            procurarJogadasLegais()

            enviaTabuleiroAosClientes()
            setStateJogador(temJogadasPossiveis())

            updateJogadasEspeciais()
        }
    }

    private fun terminaJogo() {
        state.postValue(State.GAME_ENDED)

        //*Guardar resultado firestore*//*

        //game_mode2
        //    "S" to "0:0",
        //    "adv" to " "
        //
        //game_mode3
        //    "S" to "0:0:0",
        //    "adv1" to " ",
        //    "adv2" to " "

        var stringScore: String

        if (modoJogo == 2 || modoJogo == 3) {
            var Myscore = jogadores[0].pontos.toString()
            var Advscore = jogadores[1].pontos.toString()
            stringScore = "$Myscore:$Advscore"
            var adv = jogadores[1].nome.toString()
            var adv1: String? = null
            var adv2: String? = null
            if (modoJogo == 3) {
                var Adv2score = jogadores[2].pontos.toString()
                adv1 = jogadores[1].nome.toString()
                adv2 = jogadores[2].nome.toString()
                stringScore = "$Myscore:$Advscore:$Adv2score"
            }

            var cont: String = Myscore
            var colocou: Boolean = false    //true quando foi colocado no firestore : TOP SCORE
            var index: Int =
                -1             //-1, não faz parte do top Score, >0, faz parte do top Score
            //, sendo que os scores default(0:0) já estejam todos preenchidos

            var userid = FirebaseAuth.getInstance().currentUser!!.uid

            var collection = db.collection("Users").document(userid)
                .collection("Scores_modo$modoJogo")

            //percorrer todos os resultados para verificar se o resultado faz parte de um dos 5 Top Score
            for (i in 1..5) {
                if (!colocou) {
                    collection.document("game$i")
                        .addSnapshotListener { docSS, e ->
                            if (e != null) {
                                return@addSnapshotListener
                            }
                            if (docSS != null && docSS.exists()) {                          //se existe documentos
                                if (modoJogo == 2) {
                                    if (docSS.getString("S") == "0:0") {
                                        val game_mode2 = hashMapOf(
                                            "S" to stringScore,
                                            "adv" to adv
                                        )
                                        val v = collection.document("game$i")
                                        v.set(game_mode2)

                                        colocou = true
                                    }
                                } else if (modoJogo == 3) {
                                    if (docSS.getString("S") == "0:0:0") {
                                        val game_mode3 = hashMapOf(
                                            "S" to stringScore,
                                            "adv1" to adv1,
                                            "adv2" to adv2
                                        )
                                        val v = collection.document("game$i")
                                        v.set(game_mode3)
                                        colocou = true
                                    }


                                }
                                var score = docSS.getString("S")!!
                                    .split(":")[0]        //guarda resultado jogador logado a partir do score já existente no firestore
                                if (score < cont) {
                                    cont = score    //guarda o valor do resultado mais baixo para comparar na proxima iteração: Objetivo é
                                    // guardao o index do resultado mais baixo obtido do firestore
                                    index = i       //guarda o index do jogo com o resultado mais pequeno
                                }
                            } //else
                            //Toast.makeText(this, "Error saving Score", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    return
                }
            }

            if (!colocou) {                         //se o score está cheio e não foi colocado nenhum novo score no firestore
                if (index != -1) {                  //verificar se score faz parte do TOP Score
                        if (modoJogo == 2) {
                            val game_mode2 = hashMapOf(
                                "S" to stringScore,
                                "adv" to adv
                            )
                            val v = collection.document("game$index")
                            v.set(game_mode2)
                        }else {
                            val game_mode3 = hashMapOf(
                                "S" to stringScore,
                                "adv1" to adv1,
                                "adv2" to adv2
                            )
                            val v = collection.document("game$index")
                            v.set(game_mode3)
                        }
                }
            }
        }
    }

    fun comecarJogo() {
        if (modoJogo == 1) {
            jogadores.add(Jogador(BRANCO))
            jogadores.add(Jogador(PRETO))

            jogadorAtual = Random.nextInt(0, jogadores.size)

            procurarJogadasLegais()
            setStateJogador(temJogadasPossiveis())
        }

        if (isServer) {
            jogadorAtual = Random.nextInt(0, jogadores.size)
            procurarJogadasLegais()
            setStateJogador(temJogadasPossiveis())
            if (modoJogo > 1){
                enviaTabuleiroAosClientes()
                avisaJogadorAtual()
            }
            trocaDados = TrocaDados(getCorAtual())
        }

        updatePontuacao()
    }

    private fun enviaFimDeJogoAosClientes() {
        for (j in jogadores)
            if (j.cor != BRANCO)
                enviaFimDeJogo(j.socket)
    }

    private fun enviaFimDeJogo(socket: Socket?) {
        if (socket?.getOutputStream() == null)
            return

        val jsonObject = JSONObject()

        jsonObject.put("tipo", "fimDeJogo")

        thread {
            enviaJsonObject(socket, jsonObject)
        }

    }

    fun avisaJogadorAtual() {
        Log.i(TAG, "avisaJogadorAtual: aqui devia avisar ${state.value}")

        if (state.value == State.STARTING || state.value == State.TURN_ENDED || state.value == State.GAME_ENDED)
            return
        if (state.value == State.CLIENT_PLAYING_REGULAR || state.value == State.CLIENT_PLAYING_SWAP){
            avisaJogadorPodeJogar(jogadores[jogadorAtual], true)
        }else if (state.value == State.CLIENT_UNPLAYABLE ){
            avisaJogadorPodeJogar(jogadores[jogadorAtual], false)
        }
    }

    private fun avisaJogadorPodeJogar(jogador: Jogador, podeJogar : Boolean) {
        if (jogador.socket?.getOutputStream() == null)
            return

        Log.i(TAG, "avisaJogadorPodeJogar: devia avisar aqui que pode jogar ${state.value}")
        val jsonObject = JSONObject()
        if (!podeJogar) {
            jsonObject.put("tipo", "naoPodeJogar")
        }else{
            jsonObject.put("tipo", "jogada")
            jsonObject.put("bomba", jogador.bomba && podeJogadasEspeciais())
            jsonObject.put("troca", jogador.troca && podeJogadasEspeciais())
        }
        thread {
            enviaJsonObject(jogador.socket, jsonObject)
        }


    }

    private fun setStateJogador(temJogadas: Boolean) {
        if (modoJogo == 1) {
            state.postValue(if (temJogadas) State.LOCAL_PLAYING_REGULAR else State.LOCAL_UNPLAYABLE)
        } else {
            if (jogadorAtual == 0){
                state.postValue( (if (temJogadas) State.LOCAL_PLAYING_REGULAR else State.LOCAL_UNPLAYABLE) )
                Log.i(TAG, "setStateJogador: tem jogadas ${temJogadas} local")
            }
            else{
                state.postValue( (if (temJogadas) State.CLIENT_PLAYING_REGULAR else State.CLIENT_UNPLAYABLE) )
                Log.i(TAG, "setStateJogador: tem jogadas ${temJogadas} cliente")
            }
        }
    }

    private fun updatePontuacao() {
        for (j in jogadores)
            j.pontos = 0

        for (l in tabuleiro.celulas)
            for (c in l) {
                if (c.cor != VAZIO)
                    getJogador(c.cor)!!.pontos++
            }
    }

    private fun getJogador(cor: Int): Jogador? {
        for (j in jogadores)
            if (j.cor == cor)
                return j
        return null

    }

    fun getPontos(cor: Int): Int {
        for (j in jogadores)
            if (j.cor==cor)
                return j.pontos
        return -1
    }

    fun getCorAtual(): Int {
        return jogadores[jogadorAtual].cor
    }

    fun atualTemBomba(): Boolean {
        return jogadores[jogadorAtual].bomba
    }

    fun atualTemTroca(): Boolean {
        return jogadores[jogadorAtual].troca
    }

    fun aJogarBomba(): Boolean {
        return state.value == State.LOCAL_PLAYING_BOMB
    }

    fun aJogarTroca(): Boolean {
        return state.value == State.LOCAL_PLAYING_SWAP
    }

    fun comDicas(): Boolean {
        return dicas
    }

    fun cliqueDicas() {
        dicas = !dicas
    }

    private fun ninguemPodeJogar(): Boolean {
        for (j in jogadores) {
            if (j.podeJogar)
                return false
        }
        Log.i("TAG", "ninguemPodeJogar: true")
        return true
    }


    private fun procurarJogadasLegais() {
        jogadasLegais.clear()                                                   //Limpa a lista de jogadas legais
        Log.i(TAG, "procurarJogadasLegais: procurar jogadas para ${getCorAtual()}")
        jogadores[jogadorAtual].podeJogar = true
        for (i in 0 until tabuleiro.celulas.size) {
            for (j in 0 until tabuleiro.celulas.size) {
                val dir = tabuleiro.isJogadaLegal(i, j, jogadores[jogadorAtual].cor)
                if (dir > 0) {
                    jogadasLegais.add(
                        JogadaLegal(
                            i,
                            j,
                            dir
                        )
                    )                     //Guarda as jogadas legais, com informação das coordenadas (i,j)
                }                                                   //...  e a primeira direção que torna a jogada legal (1=Topo Esquerda, ... , 8 = Baixo Direita)
            }
        }
    }

    fun cliqueBombaServer() {
        if (jogadores[jogadorAtual].bomba) {
            if (state.value == State.LOCAL_PLAYING_BOMB)
                state.postValue(State.LOCAL_PLAYING_REGULAR)
            else if (state.value == State.LOCAL_PLAYING_SWAP || state.value == State.LOCAL_PLAYING_REGULAR)
                state.postValue(State.LOCAL_PLAYING_BOMB)
        }
    }

    fun cliqueTrocaServer() {
        if (jogadores[jogadorAtual].troca) {
            if (state.value == State.LOCAL_PLAYING_SWAP)
                state.postValue(State.LOCAL_PLAYING_REGULAR)
            else if (state.value == State.LOCAL_PLAYING_BOMB || state.value == State.LOCAL_PLAYING_REGULAR) {
                preparaTroca()
                state.postValue(State.LOCAL_PLAYING_SWAP)
            }
        }
    }

    private fun preparaTroca() {
        trocaDados =
            TrocaDados(jogadores[jogadorAtual].cor)                //Cria estrutura que guarda os dados da troca a fazer
    }

    private fun proximoJogador() {
        if (++jogadorAtual >= jogadores.size)
            jogadorAtual = 0
    }

    fun temJogadasPossiveis(): Boolean {
        if (jogadasLegais.isEmpty()) {
            jogadores[jogadorAtual].podeJogar = false
            return false
        }
        return true
    }

    fun passaVez() {
        if (modoJogo == 1){
            if (state.value == State.LOCAL_UNPLAYABLE || state.value == State.CLIENT_UNPLAYABLE)
                terminaJogada()
        }else{
            if (isServer){
                terminaJogada()
            }else{
                if (clientSocket?.getOutputStream() == null)
                    return

                val jsonObject = JSONObject()

                jsonObject.put("tipo", "passaVez")

                thread {
                    enviaJsonObject(clientSocket, jsonObject)
                }
            }
        }

    }

    fun startServer() {
        isServer = true
        jogadores.clear()
        jogadores.add(Jogador(BRANCO))

        if (serverSocket != null ||
            connectionState.value != ConnectionState.SETTING_PARAMETERS
        )
            return

        connectionState.postValue(ConnectionState.SERVER_CONNECTING)


        thread {
            serverSocket = ServerSocket(SERVER_PORT)
            serverSocket?.run {
                try {
                    while (numConnected < modoJogo - 1) {
                        val socketClient = serverSocket!!.accept()
                        numConnected++
                        thread { startServerComm(socketClient) }
                    }
                } catch (_: Exception) {
                    connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                } finally {
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }

    private fun startServerComm(newSocket: Socket) {
        val j = Jogador(jogadores.size + 1)
        j.socket = newSocket

        jogadores.add(j)

        if (allConnected()){
            thread {
                Thread.sleep(1000)
                comecarJogo()
            }
        }

        Log.i("TAG", "startServerComm: ${newSocket.inetAddress}")
        j.threadComm = thread {
            try {
                if (j.socket!!.getInputStream() == null)
                    return@thread

                connectionState.postValue(ConnectionState.GETTING_INFO)

                thread {
                    serverEnviaInformacao(j.socket, j.cor)
                    enviaFirebaseImageAsByteArray(j.socket, "fotoServer")
                }

                val bufI = j.socket!!.getInputStream().bufferedReader()

                Log.i("TAG", "startServerComm: state is ${state.value}")
                while (state.value != State.GAME_ENDED) {

                    val str : String = bufI.readLine()

                    if (str.isNotEmpty()){
                        Log.i("TAG", "startServerComm:string not nempty ")
                        val message = str.toString()

                        val jsonObject = JSONObject(message)
                        serverMensagemRecebida(j, jsonObject)
                    }else{
                        Log.i("TAG", "startServerComm: string empty")
                    }
                }
                Log.i("TAG", "startServerComm: state is ${state.value}")
            } catch (e: Exception) {
                Log.i("TAG", "startServerComm:exception ${e.toString()} ")
            } finally {
                Log.i("TAG", "startServerComm: stopGame")
                stopGame()
            }
        }
    }

    private fun serverMensagemRecebida(j: Jogador, jsonObject: JSONObject) {
        val tipo = jsonObject.getString("tipo")
        Log.i("TAG", "serverMensagemRecebida: ${connectionState.value}\n $jsonObject")

        when(tipo){
            "info" -> {
                j.nome = jsonObject.getString("nome")
            }
            "foto" ->{
                val imageJSONArray = jsonObject.getJSONArray("image")
                val byteArray = imageJSONArray.toByteArray()
                j.image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                if(modoJogo == 2)
                    connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                else if (modoJogo == 3){
                    if (jogadores.size == 3) {
                        if (jogadores[1].image != null && jogadores[2].image != null){
                            connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                            thread { serverPartilhaInformacaoAdversario() }
                        }else{
                            connectionState.postValue(ConnectionState.HALF_CONNECTION_ESTABLISHED)
                        }
                    }else{
                        connectionState.postValue(ConnectionState.HALF_CONNECTION_ESTABLISHED)
                    }

                }
                state.postValue(state.value)
            }
            "passaVez" -> {
                if (state.value == State.CLIENT_UNPLAYABLE)
                    terminaJogada()
            }
            "clique" ->{
                val linha = jsonObject.getInt("linha")
                val coluna = jsonObject.getInt("coluna")
                val powerup = jsonObject.getString("powerup")
                val oldState = state.value

                when (powerup){
                    "none" -> {if (
                        tabuleiro.fazJogada(
                            linha,
                            coluna,
                            jogadores[jogadorAtual].cor,
                            jogadasLegais.find { jogadaLegal -> jogadaLegal.linha == linha && jogadaLegal.coluna == coluna }?.direcao
                        )
                    ) {
                            state.postValue(State.TURN_ENDED)
                            terminaJogada()
                    }else
                        state.postValue(oldState)
                    }
                    "bomb" -> {if (jogadores[jogadorAtual].bomba){
                        if (
                            tabuleiro.fazBomba(
                                linha,
                                coluna,
                                jogadores[jogadorAtual].cor,
                            )
                        ) {
                            jogadores[jogadorAtual].bomba = false
                            state.postValue(State.TURN_ENDED)
                            terminaJogada()
                        }else
                            state.postValue(oldState)
                        }}
                    "swap" -> {if (jogadores[jogadorAtual].troca){
                        trocaDados.sacrificio.clear()
                        val jsonArray = jsonObject.getJSONArray("sacrificio")
                        for (i in 0 until jsonArray.length()) {
                            val jsonTroca = jsonArray.getJSONObject(i)
                            Log.i(TAG, "serverMensagemRecebida: ${jsonTroca.toString()}")
                            trocaDados.sacrificio.add(Coordenadas(jsonTroca.getInt("linha"), jsonTroca.getInt("coluna")))
                        }
                        trocaDados.recompensa = Coordenadas(linha, coluna)

                        if (tabuleiro.fazTroca(trocaDados)){
                            jogadores[jogadorAtual].troca = false
                            state.postValue(State.TURN_ENDED)
                            terminaJogada()
                        }
                    }}

                }

            }

        }
    }

    private fun serverPartilhaInformacaoAdversario() {
        serverEnviaInformacaoAdversario(jogadores[1].socket, jogadores[2].nome!!)
        serverEnviaInformacaoAdversario(jogadores[2].socket, jogadores[1].nome!!)
        serverEnviaImagemAdversario(jogadores[2].socket, jogadores[1].image!!)
        serverEnviaImagemAdversario(jogadores[1].socket, jogadores[2].image!!)



    }

    private fun serverEnviaImagemAdversario(socket: Socket?, image: Bitmap) {
        val stream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val jsonObject = JSONObject()
        jsonObject.put("tipo", "fotoAdversario")
        val jsonImageArray = JSONArray(stream.toByteArray())
        jsonObject.put("image", jsonImageArray)

        thread {
            enviaJsonObject(socket, jsonObject)
        }
    }

    private fun serverEnviaInformacaoAdversario(socket: Socket?, nome: String) {
        if (socket?.getOutputStream() == null)
            return

        val jsonObject = JSONObject()

        jsonObject.put("tipo", "infoAdversario")

        jsonObject.put("nome", nome)

        thread {
            enviaJsonObject(socket, jsonObject)
        }

    }


    private fun clienteMensagemRecebida(jsonObject: JSONObject) {
        val tipo = jsonObject.getString("tipo")
        when(tipo){
            "infoServer" -> {
                val cor = jsonObject.getInt("cor")
                jogadores.add(Jogador(cor))
                jogadores[0].nome = nomeClienteTemp
                nomeClienteTemp = null

                if (jogadores.size==1)
                    jogadores.add(Jogador(BRANCO))
                jogadores[1].nome = jsonObject.getString("nome")

                if (modoJogo == 3){
                    if (cor == PRETO)
                        jogadores.add(Jogador(AZUL))
                    else
                        jogadores.add(Jogador(PRETO))
                }
            }
            "fotoServer" -> {
                val imageJSONArray = jsonObject.getJSONArray("image")
                val imageByteArray = imageJSONArray.toByteArray()

                if (jogadores.size==1)
                    jogadores.add(Jogador(BRANCO))
                jogadores[1].image = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                if(modoJogo == 2)
                    connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                else if (modoJogo == 3){
                    if (connectionState.value == ConnectionState.HALF_CONNECTION_ESTABLISHED){
                        connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                        thread { serverPartilhaInformacaoAdversario() }
                    }
                    else
                        connectionState.postValue(ConnectionState.HALF_CONNECTION_ESTABLISHED)
                }
            }
            "infoAdversario" -> {
                if (modoJogo == 3)
                    jogadores[2].nome = jsonObject.getString("nome")
            }
            "fotoAdversario" -> {
                if (modoJogo == 3){
                    val imageJSONArray = jsonObject.getJSONArray("image")
                    val imageByteArray = imageJSONArray.toByteArray()

                    jogadores[2].image = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                    connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                }
            }
            "tabuleiro" -> {
                Log.i(TAG, "clienteMensagemRecebida: recebi tabuleiro")
                val tabuleiroRecebido = (jsonObject.getJSONArray("tabuleiro")).toIntArray()
                var it = 0
                for (l in tabuleiro.celulas)
                    for (c in l){
                        c.cor = tabuleiroRecebido[it++]
                        Log.i(TAG, "TABULEIRO peça ${c.coordenadas.linha}, ${c.coordenadas.coluna} -> ${c.cor}")
                    }
                val corRecebida = jsonObject.getInt("corAtual")
                for (i in 0 until jogadores.size)
                    if (jogadores[i].cor == corRecebida){
                        jogadorAtual = i
                        break
                    }

                updatePontuacao()

                state.postValue(State.TURN_ENDED)
            }
            "naoPodeJogar" -> {
                state.postValue(State.LOCAL_UNPLAYABLE)
            }
            "jogada" -> {
                jogadorAtual = 0
                jogadores[jogadorAtual].bomba = jsonObject.getBoolean("bomba")
                jogadores[jogadorAtual].troca = jsonObject.getBoolean("troca")
                procurarJogadasLegais()
                state.postValue(State.LOCAL_PLAYING_REGULAR)
            }
            "fimDeJogo" -> {
                state.postValue(State.GAME_ENDED)
            }

        }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }

    fun stopGame() {
        try {
            state.postValue(State.GAME_ENDED)
            connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            for (j in jogadores) {
                j.socket?.close()
                j.socket = null
                j.threadComm?.interrupt()
                j.threadComm = null
            }
        } catch (_: Exception) {
        }
    }

    fun startClient(serverIP: String, serverPort: Int = SERVER_PORT) {
        isServer = false
        jogadores.clear()
        jogadasEspeciais = true

        if (connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        thread {
            connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                //val newsocket = Socket(serverIP, serverPort)
                val newsocket = Socket()
                newsocket.connect(InetSocketAddress(serverIP, serverPort), 5000)
                startClientComm(newsocket)
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                stopGame()
            }
        }
    }

    private fun startClientComm(newSocket: Socket) {
        if (clientThreadComm != null)
            return

        clientSocket = newSocket

        clientThreadComm = thread {
            try {
                if (clientSocket!!.getInputStream() == null)
                    return@thread

                Log.i(TAG, "b4 Mensagem: state is ${state.value}")
                if (connectionState.value != ConnectionState.HALF_CONNECTION_ESTABLISHED)
                    connectionState.postValue(ConnectionState.GETTING_INFO)
                val bufI = clientSocket!!.getInputStream().bufferedReader()

                thread {
                    clienteEnviaInformacao(clientSocket)
                    enviaFirebaseImageAsByteArray(clientSocket, "foto")
                }

                Log.i(TAG, "startClientComm: state is ${state.value}")
                while (state.value != State.GAME_ENDED) {
                    Log.i(TAG, "startClientComm: state is entrei no while")
                    val str : String = bufI.readLine()

                    if (str.isNotEmpty()) {
                        Log.i(TAG, "startClientComm: mensagem recebida")
                        val message = str.toString()

                        val jsonObject = JSONObject(message)

                        Log.i(TAG, "startClientComm: mensagem recebida: $jsonObject")

                        clienteMensagemRecebida(jsonObject)
                    }
                }
                Log.i(TAG, "startClientComm: state is ${state.value}")
            } catch (e: Exception) {
                Log.i(TAG, "startClientComm: ${e.toString()}")
            } finally {
                stopGame()
            }
        }
    }


    private fun serverEnviaInformacao(socket: Socket?, cor: Int) {
        if (socket?.getOutputStream() == null)
            return

        val jsonObject = JSONObject()

        jsonObject.put("cor", cor)

        jsonObject.put("tipo", "infoServer")

        val nome = getNomeFirebase() ?: return
        jsonObject.put("nome", nome)
        jogadores[0].nome = nome

        thread {
            enviaJsonObject(socket, jsonObject)
        }

    }

    private fun clienteEnviaInformacao(socket: Socket?) {
        if (socket?.getOutputStream() == null)
            return

        val jsonObject = JSONObject()

        jsonObject.put("tipo", "info")

        val nome = getNomeFirebase() ?: return
        jsonObject.put("nome", nome)

        nomeClienteTemp = nome

        thread {
            enviaJsonObject(socket, jsonObject)
        }
    }

    private fun enviaJsonObject(socket: Socket?, jsonObject: JSONObject) {
        if (socket?.getOutputStream() == null)
            return

        socket.getOutputStream().run {
            val printStream = PrintStream(this)
            printStream.println(jsonObject.toString())
            printStream.flush()
            Log.i("TAG", "enviaJsonObject: enviei ${jsonObject.toString()}")
        }
    }

    private fun enviaFirebaseImageAsByteArray(socket: Socket?, tipo : String) {
        val auth = FirebaseAuth.getInstance()

        val user = auth.currentUser ?: return

        try {
            val storageReference = FirebaseStorage.getInstance().reference.child("Users/"+user.uid+".jpg")
            val localfile = File.createTempFile("temporary","jpg")
            storageReference.getFile(localfile).addOnSuccessListener {
                val stream = ByteArrayOutputStream()
                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val jsonObject = JSONObject()
                jsonObject.put("tipo", tipo)
                val jsonImageArray = JSONArray(stream.toByteArray())
                jsonObject.put("image", jsonImageArray)
                jogadores[0].image = bitmap
                thread {
                    enviaJsonObject(socket, jsonObject)
                }
            }.addOnFailureListener{
                val stream = ByteArrayOutputStream()
                defaultImage!!.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val jsonObject = JSONObject()
                jsonObject.put("tipo", tipo)
                val jsonImageArray = JSONArray(stream.toByteArray())
                jsonObject.put("image", jsonImageArray)
                jogadores[0].image = defaultImage
                thread {
                    enviaJsonObject(socket, jsonObject)
                }
            }
        }catch (_: java.lang.Exception){
        }
        return
    }

    private fun getNomeFirebase(): String? {
        val auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        return user?.displayName

    }

    private fun enviaTabuleiroAosClientes() {
        for (j in jogadores)
            if (j.cor != BRANCO)
                enviaTabuleiro(j.socket)
    }

    fun enviaTabuleiro(socket: Socket?){
        val jsonObject = JSONObject()
        val jsonArray = JSONArray()
        jsonObject.put("tipo", "tabuleiro")
        for (l in tabuleiro.celulas){
            for (c in l){
                jsonArray.put(c.cor)
            }
        }
        jsonObject.put("tabuleiro", jsonArray)
        jsonObject.put("corAtual", getCorAtual())

        thread {
            enviaJsonObject(socket, jsonObject)
        }
    }

    fun allConnected(): Boolean {
        return (modoJogo != 1 && numConnected == modoJogo-1)
    }

    fun JSONArray.toByteArray(): ByteArray {
        val byteArr = ByteArray(length())
        for (i in 0 until length()) {
            byteArr[i] = (get(i) as Int).toByte()
        }
        return byteArr
    }
    fun JSONArray.toIntArray(): IntArray {
        val intArr = IntArray(length())
        for (i in 0 until length()) {
            intArr[i] = getInt(i)
        }
        return intArr
    }

    fun podeJogadasEspeciais(): Boolean {
        return jogadasEspeciais
    }

    private fun updateJogadasEspeciais(){

        jogadasEspeciais = jogadas.toFloat() / jogadores.size >= 4
    }

    fun stateIsLocal(): Boolean {
        return state.value == State.LOCAL_PLAYING_REGULAR ||
                state.value == State.LOCAL_PLAYING_SWAP ||
                state.value == State.LOCAL_PLAYING_BOMB ||
                state.value == State.LOCAL_UNPLAYABLE
    }
}





