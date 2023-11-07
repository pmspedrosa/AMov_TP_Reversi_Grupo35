package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick_btnModo1(view: View) {
        val myIntent = Intent(this, GameActivity::class.java)
        myIntent.putExtra("MODO", 1)

        startActivity(myIntent)
        }
    fun onClick_btnModo2(view: View) {
        val dlg =  AlertDialog.Builder(this)
            .setTitle(supportActionBar?.title)
            .setMessage(getString(R.string.msg_modo2))
            .setNeutralButton(getString(R.string.criar_jogo)){ d, w ->
                val myIntent = Intent(this, GameActivity::class.java)
                myIntent.putExtra("MODO", 2)
                myIntent.putExtra("SERVER", true)

                startActivity(myIntent)
            }
            .setPositiveButton(getString(R.string.juntar_jogo)){ d, w ->
                val myIntent = Intent(this, GameActivity::class.java)
                myIntent.putExtra("MODO", 2)
                myIntent.putExtra("SERVER", false)

                startActivity(myIntent)
            }
            .create()
        dlg.show()
    }
    fun onClick_btnModo3(view: View) {
        val dlg =  AlertDialog.Builder(this)
            .setTitle(supportActionBar?.title)
            .setMessage(getString(R.string.msg_modo3))
            .setNeutralButton(getString(R.string.criar_jogo)){ d, w ->
                val myIntent = Intent(this, GameActivity::class.java)
                myIntent.putExtra("MODO", 3)
                myIntent.putExtra("SERVER", true)

                startActivity(myIntent)
            }
            .setPositiveButton(getString(R.string.juntar_jogo)){ d, w ->
                val myIntent = Intent(this, GameActivity::class.java)
                myIntent.putExtra("MODO", 3)
                myIntent.putExtra("SERVER", false)

                startActivity(myIntent)
            }
            .create()
        dlg.show()
    }
    fun onClick_btnPerfil(view: View) {
        val myIntent = Intent(this, PerfilActivity::class.java)
        startActivity(myIntent)
    }
    fun onClick_btnCreditos(view: View) {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        //finish()
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.mnPerfil -> {
                val intent = Intent(this, PerfilActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.mnLogout -> {
                val dlg =  AlertDialog.Builder(this)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_questao)
                    .setPositiveButton(R.string.sim){
                            d,w ->
                        FirebaseAuth.getInstance().signOut()

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton(R.string.nao){
                            dialog, w -> dialog.dismiss()
                    }
                    .setCancelable(false)
                    .create()
                dlg.show()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }
}