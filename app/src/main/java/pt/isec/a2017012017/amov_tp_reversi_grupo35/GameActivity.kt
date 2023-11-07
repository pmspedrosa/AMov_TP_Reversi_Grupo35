package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import pt.isec.a2017012017.amov_tp_reversi_grupo35.dados.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import android.util.DisplayMetrics
import com.google.firebase.auth.FirebaseAuth
import java.security.AccessController.getContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class GameActivity : AppCompatActivity() {
    val TAG = "tag"
    var usableHeight = 0
    var usableWidth = 0
    private val model: GameViewModel by viewModels()
    private var connect_dlg : AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        model.modoJogo = intent.getIntExtra("MODO", 1)

        when(model.modoJogo){
            1 -> {
                setContentView(R.layout.activity_gamemode1e2)
                adicionarTabuleiro_8()
                model.tabuleiro.iniciar2Jogadores()

                findViewById<Button>(R.id.btn_Bomba).setOnClickListener{ onClick_btnBomba_Server()}
                findViewById<Button>(R.id.btn_Troca).setOnClickListener{ onClick_BtnTroca_Server()}

                model.comecarJogo()
            }
            2 -> {
                setContentView(R.layout.activity_gamemode1e2)
                adicionarTabuleiro_8()
                model.tabuleiro.iniciar2Jogadores()
                model.defaultImage = BitmapFactory.decodeResource(resources, R.drawable.img_nperfil)

                if (intent.getBooleanExtra("SERVER", false)){
                    comecarServidor()
                }else{
                    comecarCliente()
                }
            }
            3 -> {
                setContentView(R.layout.activity_gamemode3)
                adicionarTabuleiro_10()

                model.tabuleiro.iniciar3Jogadores()
                model.defaultImage = BitmapFactory.decodeResource(resources, R.drawable.img_nperfil)


                if (intent.getBooleanExtra("SERVER", false)){
                    comecarServidor()
                }else {
                    comecarCliente()
                }
            }
            else -> {
                //sair
            }
        }
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        usableHeight = size.y
        usableWidth = size.x
        //resizeTabuleiro()


        setClickListeners()

        model.state.observe(this){
            Log.i(TAG, "observer has observed")
            Log.i(TAG, "onCreate: ${model.connectionState.value}")
            Log.i(TAG, "onCreate: ${model.state.value}")

            if ((model.modoJogo == 1) || ((model.allConnected() || !model.isServer) &&
                (model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED || model.connectionState.value == GameViewModel.ConnectionState.GETTING_INFO))) {
                    if (connect_dlg?.isShowing == true) {
                        Log.i(TAG, "connect_dlg is showing ")
                        connect_dlg?.dismiss()
                        connect_dlg = null
                    }
                    updateUI()
                }
            if (model.isServer && (model.state.value == GameViewModel.State.CLIENT_PLAYING_REGULAR ||
                        model.state.value == GameViewModel.State.CLIENT_PLAYING_SWAP ||
                        model.state.value == GameViewModel.State.CLIENT_UNPLAYABLE ))
                        {
                model.avisaJogadorAtual()

            }

        }
        model.connectionState.observe(this){
            if (model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED){}
                //model.comecarJogo()
            }

        }

    private fun comecarCliente() {
        val edtBox = EditText(this).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(
                    source: CharSequence?,
                    start: Int,
                    end: Int,
                    dest: Spanned?,
                    dstart: Int,
                    dend: Int
                ): CharSequence? {
                    source?.run {
                        var ret = ""
                        forEach {
                            if (it.isDigit() || it.equals('.'))
                                ret += it
                        }
                        return ret
                    }
                    return null
                }

            })
        }
        val dlg = AlertDialog.Builder(this)
            .setTitle(getString(R.string.client_mode))
            .setMessage(getString(R.string.ask_ip))
            .setPositiveButton(getString(R.string.button_connect)) { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(this@GameActivity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    model.startClient(strIP)
                }
            }
            .setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                model.startClient("10.0.2.2", SERVER_PORT-1)
                // Configure port redirect on the Server Emulator:
                // telnet localhost <5554|5556|5558|...>
                // auth <key>
                // redir add tcp:9998:9999
            }
            .setNegativeButton(getString(R.string.button_cancel)) { _: DialogInterface, _: Int ->
                finish()
            }
            .setCancelable(false)
            .setView(edtBox)
            .create()

        dlg.show()
    }

    private fun comecarServidor() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress // Deprecated in API Level 31. Suggestion NetworkCallback
        val strIPAddress = String.format("%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )

        val ll = LinearLayout(this).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = paramsTV
                text = String.format(getString(R.string.msg_ip_address),strIPAddress)
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        connect_dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.server_mode))
            .setView(ll)
            .setOnCancelListener {
                model.stopServer()
                finish()
            }
            .create()

        model.startServer()

        connect_dlg?.show()
    }

    private fun updateUI() {
        updateTabuleiro()
        if (model.state.value == GameViewModel.State.GAME_ENDED){
            Log.i(TAG, "updateUI: terminaJOGO")
            terminaJogo()
        }else {
            Log.i(TAG, "updateUI: updating UI")
            updateDicasDisplay()
            updateTrocaDisplay()
            updateDisplayPontuacao()
            updateBotoes()
            prepararClique()
        }
    }

    private fun updateTrocaDisplay() {
        if (model.state.value == GameViewModel.State.LOCAL_PLAYING_SWAP || model.state.value == GameViewModel.State.CLIENT_PLAYING_SWAP || model.state.value == GameViewModel.State.TURN_ENDED)
            for (coords in model.trocaDados!!.sacrificio)
                model.tabuleiro.getCelula(coords).imageButton.setBackgroundColor(resources.getColor(R.color.dark_yellow))

    }

    private fun updateTabuleiro() {
        Log.i(TAG, "updateTabuleiro: updating tabulkeiro")
        for (r in model.tabuleiro.celulas){                                           //Coloca as imagens das peças nas células
            for (c in r){
                when(c.cor) {
                    VAZIO -> c.imageButton.setImageResource(R.drawable.vazio)
                    BRANCO -> c.imageButton.setImageResource(R.drawable.bola_branca)
                    PRETO -> c.imageButton.setImageResource(R.drawable.bola_preta)
                    AZUL -> c.imageButton.setImageResource(R.drawable.bola_azul)
                }
                c.imageButton.setBackgroundColor(resources.getColor(R.color.cor_tabuleiro))
            }
        }
    }

    private fun updateDicasDisplay() {
        Log.i(TAG, "updateDicasDisplay: ${model.state.value}")
        if (model.state.value == GameViewModel.State.LOCAL_PLAYING_REGULAR){
            if (model.comDicas()){
                Log.i(TAG, "updateDicasDisplay: ")
                for (j in model.jogadasLegais){
                    model.tabuleiro.celulas[j.linha][j.coluna].imageButton.setImageResource(R.drawable.jogada_legal)          //Coloca uma imagem nas celulas com jogadas legais
                }
            }else{
                for (j in model.jogadasLegais){
                    model.tabuleiro.celulas[j.linha][j.coluna].imageButton.setImageResource(R.drawable.vazio)          //Coloca uma imagem nas celulas com jogadas legais
                }
            }
        }

    }

    private fun updateDisplayPontuacao() {
        findViewById<TextView>(R.id.pontuacao_branco).text=model.getPontos(BRANCO).toString()
        findViewById<TextView>(R.id.pontuacao_preto).text=model.getPontos(PRETO).toString()
        if (model.modoJogo == 3)
            findViewById<TextView>(R.id.pontuacao_azul).text=model.getPontos(AZUL).toString()

        if (model.modoJogo>1 && !model.isServer){
            when (model.getCorAtual()) {
                BRANCO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_branca)
                PRETO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_preta)
                AZUL -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_azul)
            }
        }
        when(model.state.value){
            GameViewModel.State.LOCAL_PLAYING_BOMB, GameViewModel.State.CLIENT_PLAYING_BOMB  ->{
                when (model.getCorAtual()) {
                    BRANCO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bomba_branca)
                    PRETO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bomba_preta)
                    AZUL -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bomba_azul)
                }
            }
            GameViewModel.State.LOCAL_PLAYING_SWAP, GameViewModel.State.CLIENT_PLAYING_SWAP ->{
                when (model.getCorAtual()) {
                    BRANCO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.trade_branco)
                    PRETO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.trade_preto)
                    AZUL -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.trade_azul)
                }
            }
            GameViewModel.State.LOCAL_PLAYING_REGULAR, GameViewModel.State.CLIENT_PLAYING_REGULAR -> {
                when(model.getCorAtual()){                                      //Altera a imagem que indicadica o jogador atual
                    BRANCO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_branca)
                    PRETO -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_preta)
                    AZUL -> findViewById<ImageView>(R.id.iV_player_atual).setImageResource(R.drawable.bola_azul)
                }
            }
        }

        for (j in model.jogadores)
            when (j.cor){
                BRANCO ->{ if (j.nome!= null && j.image != null)
                    findViewById<TextView>(R.id.txt_branco).setText(j.nome)
                    findViewById<ImageView>(R.id.iV_layout_branco).setImageBitmap(j.image)
                }
                PRETO -> { if (j.nome!= null && j.image != null)
                    findViewById<TextView>(R.id.txt_preto).setText(j.nome)
                    findViewById<ImageView>(R.id.iV_layout_preto).setImageBitmap(j.image)
                }
                AZUL -> { if (j.nome!= null && j.image != null)
                    findViewById<TextView>(R.id.txt_azul)?.setText(j.nome)
                    findViewById<ImageView>(R.id.iV_layout_azul).setImageBitmap(j.image)
                }

            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (model.modoJogo == 1)
            menuInflater.inflate(R.menu.game_menu_sp, menu)
        else
            menuInflater.inflate(R.menu.game_menu_mp, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.mnNovoJogo -> {
                onNovoJogobtn()
                return true
            }
            R.id.mnRegras -> {
                val intent = Intent(this, RulesActivity::class.java)
                startActivity(intent)
                //finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setClickListeners() {
        for (i in 0 until model.tabuleiro.getTam()){
            for(j in 0 until model.tabuleiro.getTam()){
                model.tabuleiro.celulas[i][j].imageButton.setOnClickListener {
                    model.trataClique(i,j)
                }
            }
        }
        findViewById<Button>(R.id.btn_Bomba).setOnClickListener{ onClick_btnBomba_Server()}
        findViewById<Button>(R.id.btn_Troca).setOnClickListener{ onClick_BtnTroca_Server()}
    }

    private fun terminaJogo() {
        Log.i(TAG, "terminaJogo: ")
        //se online, enviar coisas ao adversário

        //enviar resultados para firebase

        //criar alert dialog para mostrar resultado final.
        //alert dialog tem botao para ir para menu e para começar novo jogo
        val dialogView = LayoutInflater.from(this).inflate(if (model.modoJogo == 3) R.layout.dialog_fim_modo3 else R.layout.dialog_fim_modo1e2, null)

        val dlgBuilder = AlertDialog.Builder(this)
            .setTitle(supportActionBar?.title)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(R.string.menuprincipal) { d, w ->
                finish()
            }
        if (model.modoJogo == 1){
            dlgBuilder.setPositiveButton(R.string.novoJogo) { d, w ->
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        val dlg = dlgBuilder.create()

        dlg.show()
        dlg.window!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.green)))

        for(j in model.jogadores) {
            when (j.cor) {
                BRANCO -> dlg.window!!.findViewById<TextView>(R.id.tv_fim_branco)
                    .setText(j.pontos.toString())
                PRETO -> dlg.window!!.findViewById<TextView>(R.id.tv_fim_preto)
                    .setText(j.pontos.toString())
                AZUL -> {
                    if (model.modoJogo == 3)
                        dlg.window!!.findViewById<TextView>(R.id.tv_fim_azul)
                            .setText(j.pontos.toString())
                }
            }
        }

        dlg.window!!.setLayout(5 * usableWidth / 6, 2 * usableHeight / 3)
    }
    
    private fun prepararClique() {  // DIALOG "NAO PODE JOGAR"
        if(model.state.value == GameViewModel.State.LOCAL_UNPLAYABLE) {
            val dlg = AlertDialog.Builder(this)
                .setTitle(supportActionBar?.title)
                .setMessage(R.string.sem_jogada_possivel)
                .setIcon(
                    when (model.getCorAtual()) {
                        BRANCO -> R.drawable.bola_branca
                        PRETO -> R.drawable.bola_preta
                        AZUL -> R.drawable.bola_azul
                        else -> R.drawable.vazio
                    }
                )
                .setPositiveButton(R.string.passar_vez) { d, w ->
                    model.passaVez()
                }
                .setCancelable(false)
                .create()
            dlg.show()
        }
    }

    private fun adicionarTabuleiro_8() {
        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid11) as ImageButton, Coordenadas(0, 0)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid12) as ImageButton, Coordenadas(0, 1)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid13) as ImageButton, Coordenadas(0, 2)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid14) as ImageButton, Coordenadas(0, 3)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid15) as ImageButton, Coordenadas(0, 4)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid16) as ImageButton, Coordenadas(0, 5)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid17) as ImageButton, Coordenadas(0, 6)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid18) as ImageButton, Coordenadas(0, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid21) as ImageButton, Coordenadas(1, 0)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid22) as ImageButton, Coordenadas(1, 1)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid23) as ImageButton, Coordenadas(1, 2)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid24) as ImageButton, Coordenadas(1, 3)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid25) as ImageButton, Coordenadas(1, 4)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid26) as ImageButton, Coordenadas(1, 5)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid27) as ImageButton, Coordenadas(1, 6)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid28) as ImageButton, Coordenadas(1, 7)))


        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid31) as ImageButton, Coordenadas(2, 0)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid32) as ImageButton, Coordenadas(2, 1)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid33) as ImageButton, Coordenadas(2, 2)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid34) as ImageButton, Coordenadas(2, 3)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid35) as ImageButton, Coordenadas(2, 4)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid36) as ImageButton, Coordenadas(2, 5)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid37) as ImageButton, Coordenadas(2, 6)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid38) as ImageButton, Coordenadas(2, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid41) as ImageButton, Coordenadas(3, 0)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid42) as ImageButton, Coordenadas(3, 1)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid43) as ImageButton, Coordenadas(3, 2)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid44) as ImageButton, Coordenadas(3, 3)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid45) as ImageButton, Coordenadas(3, 4)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid46) as ImageButton, Coordenadas(3, 5)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid47) as ImageButton, Coordenadas(3, 6)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid48) as ImageButton, Coordenadas(3, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid51) as ImageButton, Coordenadas(4, 0)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid52) as ImageButton, Coordenadas(4, 1)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid53) as ImageButton, Coordenadas(4, 2)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid54) as ImageButton, Coordenadas(4, 3)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid55) as ImageButton, Coordenadas(4, 4)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid56) as ImageButton, Coordenadas(4, 5)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid57) as ImageButton, Coordenadas(4, 6)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid58) as ImageButton, Coordenadas(4, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid61) as ImageButton, Coordenadas(5, 0)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid62) as ImageButton, Coordenadas(5, 1)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid63) as ImageButton, Coordenadas(5, 2)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid64) as ImageButton, Coordenadas(5, 3)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid65) as ImageButton, Coordenadas(5, 4)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid66) as ImageButton, Coordenadas(5, 5)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid67) as ImageButton, Coordenadas(5, 6)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid68) as ImageButton, Coordenadas(5, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid71) as ImageButton, Coordenadas(6, 0)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid72) as ImageButton, Coordenadas(6, 1)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid73) as ImageButton, Coordenadas(6, 2)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid74) as ImageButton, Coordenadas(6, 3)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid75) as ImageButton, Coordenadas(6, 4)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid76) as ImageButton, Coordenadas(6, 5)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid77) as ImageButton, Coordenadas(6, 6)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid78) as ImageButton, Coordenadas(6, 7)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid81) as ImageButton, Coordenadas(7, 0)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid82) as ImageButton, Coordenadas(7, 1)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid83) as ImageButton, Coordenadas(7, 2)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid84) as ImageButton, Coordenadas(7, 3)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid85) as ImageButton, Coordenadas(7, 4)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid86) as ImageButton, Coordenadas(7, 5)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid87) as ImageButton, Coordenadas(7, 6)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid88) as ImageButton, Coordenadas(7, 7)))
    }

    private fun adicionarTabuleiro_10() {
        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid11) as ImageButton, Coordenadas(0, 0)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid12) as ImageButton, Coordenadas(0, 1)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid13) as ImageButton, Coordenadas(0, 2)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid14) as ImageButton, Coordenadas(0, 3)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid15) as ImageButton, Coordenadas(0, 4)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid16) as ImageButton, Coordenadas(0, 5)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid17) as ImageButton, Coordenadas(0, 6)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid18) as ImageButton, Coordenadas(0, 7)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid19) as ImageButton, Coordenadas(0, 8)))
        model.tabuleiro.celulas[0].add(CelulaTabuleiro(findViewById<View>(R.id.grid110) as ImageButton, Coordenadas(0, 9)))


        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid21) as ImageButton, Coordenadas(1, 0)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid22) as ImageButton, Coordenadas(1, 1)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid23) as ImageButton, Coordenadas(1, 2)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid24) as ImageButton, Coordenadas(1, 3)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid25) as ImageButton, Coordenadas(1, 4)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid26) as ImageButton, Coordenadas(1, 5)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid27) as ImageButton, Coordenadas(1, 6)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid28) as ImageButton, Coordenadas(1, 7)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid29) as ImageButton, Coordenadas(1, 8)))
        model.tabuleiro.celulas[1].add(CelulaTabuleiro(findViewById<View>(R.id.grid210) as ImageButton, Coordenadas(1, 9)))


        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid31) as ImageButton, Coordenadas(2, 0)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid32) as ImageButton, Coordenadas(2, 1)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid33) as ImageButton, Coordenadas(2, 2)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid34) as ImageButton, Coordenadas(2, 3)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid35) as ImageButton, Coordenadas(2, 4)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid36) as ImageButton, Coordenadas(2, 5)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid37) as ImageButton, Coordenadas(2, 6)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid38) as ImageButton, Coordenadas(2, 7)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid39) as ImageButton, Coordenadas(2, 8)))
        model.tabuleiro.celulas[2].add(CelulaTabuleiro(findViewById<View>(R.id.grid310) as ImageButton, Coordenadas(2, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid41) as ImageButton, Coordenadas(3, 0)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid42) as ImageButton, Coordenadas(3, 1)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid43) as ImageButton, Coordenadas(3, 2)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid44) as ImageButton, Coordenadas(3, 3)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid45) as ImageButton, Coordenadas(3, 4)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid46) as ImageButton, Coordenadas(3, 5)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid47) as ImageButton, Coordenadas(3, 6)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid48) as ImageButton, Coordenadas(3, 7)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid49) as ImageButton, Coordenadas(3, 8)))
        model.tabuleiro.celulas[3].add(CelulaTabuleiro(findViewById<View>(R.id.grid410) as ImageButton, Coordenadas(3, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid51) as ImageButton, Coordenadas(4, 0)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid52) as ImageButton, Coordenadas(4, 1)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid53) as ImageButton, Coordenadas(4, 2)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid54) as ImageButton, Coordenadas(4, 3)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid55) as ImageButton, Coordenadas(4, 4)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid56) as ImageButton, Coordenadas(4, 5)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid57) as ImageButton, Coordenadas(4, 6)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid58) as ImageButton, Coordenadas(4, 7)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid59) as ImageButton, Coordenadas(4, 8)))
        model.tabuleiro.celulas[4].add(CelulaTabuleiro(findViewById<View>(R.id.grid510) as ImageButton, Coordenadas(4, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid61) as ImageButton, Coordenadas(5, 0)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid62) as ImageButton, Coordenadas(5, 1)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid63) as ImageButton, Coordenadas(5, 2)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid64) as ImageButton, Coordenadas(5, 3)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid65) as ImageButton, Coordenadas(5, 4)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid66) as ImageButton, Coordenadas(5, 5)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid67) as ImageButton, Coordenadas(5, 6)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid68) as ImageButton, Coordenadas(5, 7)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid69) as ImageButton, Coordenadas(5, 8)))
        model.tabuleiro.celulas[5].add(CelulaTabuleiro(findViewById<View>(R.id.grid610) as ImageButton, Coordenadas(5, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid71) as ImageButton, Coordenadas(6, 0)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid72) as ImageButton, Coordenadas(6, 1)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid73) as ImageButton, Coordenadas(6, 2)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid74) as ImageButton, Coordenadas(6, 3)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid75) as ImageButton, Coordenadas(6, 4)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid76) as ImageButton, Coordenadas(6, 5)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid77) as ImageButton, Coordenadas(6, 6)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid78) as ImageButton, Coordenadas(6, 7)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid79) as ImageButton, Coordenadas(6, 8)))
        model.tabuleiro.celulas[6].add(CelulaTabuleiro(findViewById<View>(R.id.grid710) as ImageButton, Coordenadas(6, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid81) as ImageButton, Coordenadas(7, 0)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid82) as ImageButton, Coordenadas(7, 1)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid83) as ImageButton, Coordenadas(7, 2)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid84) as ImageButton, Coordenadas(7, 3)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid85) as ImageButton, Coordenadas(7, 4)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid86) as ImageButton, Coordenadas(7, 5)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid87) as ImageButton, Coordenadas(7, 6)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid88) as ImageButton, Coordenadas(7, 7)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid89) as ImageButton, Coordenadas(7, 8)))
        model.tabuleiro.celulas[7].add(CelulaTabuleiro(findViewById<View>(R.id.grid810) as ImageButton, Coordenadas(7, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid91) as ImageButton, Coordenadas(8, 0)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid92) as ImageButton, Coordenadas(8, 1)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid93) as ImageButton, Coordenadas(8, 2)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid94) as ImageButton, Coordenadas(8, 3)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid95) as ImageButton, Coordenadas(8, 4)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid96) as ImageButton, Coordenadas(8, 5)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid97) as ImageButton, Coordenadas(8, 6)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid98) as ImageButton, Coordenadas(8, 7)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid99) as ImageButton, Coordenadas(8, 8)))
        model.tabuleiro.celulas[8].add(CelulaTabuleiro(findViewById<View>(R.id.grid910) as ImageButton, Coordenadas(8, 9)))

        model.tabuleiro.celulas.add(mutableListOf())
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid101) as ImageButton, Coordenadas(9, 0)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid102) as ImageButton, Coordenadas(9, 1)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid103) as ImageButton, Coordenadas(9, 2)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid104) as ImageButton, Coordenadas(9, 3)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid105) as ImageButton, Coordenadas(9, 4)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid106) as ImageButton, Coordenadas(9, 5)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid107) as ImageButton, Coordenadas(9, 6)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid108) as ImageButton, Coordenadas(9, 7)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid109) as ImageButton, Coordenadas(9, 8)))
        model.tabuleiro.celulas[9].add(CelulaTabuleiro(findViewById<View>(R.id.grid1010) as ImageButton, Coordenadas(9, 9)))

    }

    fun onClick_btnBomba_Server() {
        model.cliqueBombaServer()
    }

    fun onClick_BtnTroca_Server() {
        model.cliqueTrocaServer()
    }

    private fun updateBotoes() {
        if ((model.atualTemBomba() && model.podeJogadasEspeciais()) && model.stateIsLocal()){
            findViewById<Button>(R.id.btn_Bomba).setBackgroundColor(resources.getColor(if (model.aJogarBomba()) R.color.dark_green_selected else R.color.dark_green))
            findViewById<Button>(R.id.btn_Bomba).isClickable = true
        }else{
            findViewById<Button>(R.id.btn_Bomba).setBackgroundColor(resources.getColor(R.color.grey))
            findViewById<Button>(R.id.btn_Bomba).isClickable = false
        }

        if ((model.atualTemTroca() && model.podeJogadasEspeciais()) && model.stateIsLocal()){
            findViewById<Button>(R.id.btn_Troca).setBackgroundColor(resources.getColor(if (model.aJogarTroca()) R.color.dark_green_selected else R.color.dark_green))
            findViewById<Button>(R.id.btn_Troca).isClickable = true

        }else{
            findViewById<Button>(R.id.btn_Troca).setBackgroundColor(resources.getColor(R.color.grey))
            findViewById<Button>(R.id.btn_Troca).isClickable = false
        }
        if (model.stateIsLocal()){
            findViewById<Button>(R.id.btn_Hint).isClickable = true
            findViewById<Button>(R.id.btn_Hint).setBackgroundColor(resources.getColor(if (model.comDicas()) R.color.dark_green_selected else R.color.dark_green))

        }else
        {
            findViewById<Button>(R.id.btn_Hint).isClickable = false
            findViewById<Button>(R.id.btn_Hint).setBackgroundColor(resources.getColor(R.color.dark_green))
        }


    }

    fun onClick_btnDica(view: View) {
        model.cliqueDicas()
        updateBotoes()
        updateDicasDisplay()
    }


    private fun onSair(){
        val dlg =  AlertDialog.Builder(this)
            .setTitle(supportActionBar?.title)
            .setMessage(R.string.quermesmosair)
            .setPositiveButton(R.string.sim){
                    d,w ->
                finish()
            }
            .setNegativeButton(R.string.nao){
                    dialog, w -> dialog.dismiss()
            }
            //.setIcon()
            .setCancelable(false)
            .create()
        dlg.show()
    }

    private fun onNovoJogobtn(){
        val dlg =  AlertDialog.Builder(this)
            .setTitle(supportActionBar?.title)
            .setMessage(R.string.novoJogo)
            .setPositiveButton(R.string.sim){
                    d,w ->
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton(R.string.nao){
                    dialog, w -> dialog.dismiss()
            }
            //.setIcon()
            .setCancelable(false)
            .create()
        dlg.show()
    }

    override fun onBackPressed() {
        onSair()
    }

    override fun onSupportNavigateUp(): Boolean {
        onSair()
        return false
    }

}

