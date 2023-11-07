package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_esqueceupass.*

class EsqueceuPassActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esqueceupass)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btn_enviar.setOnClickListener{
            val email: String = ed_esqueceupass_email.text.toString().trim{it<= ' '}
            if(email.isEmpty()){
                Toast.makeText(this, R.string.porfavor_escreva_email, Toast.LENGTH_SHORT).show()
            }else{
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener{task ->
                        if(task.isSuccessful) {
                            Toast.makeText(
                                this,
                                R.string.enviarsucesso,
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }else{
                            Toast.makeText(this, task.exception!!.message.toString(), Toast.LENGTH_LONG).show()

                        }
                    }
            }
        }


    }

    override fun onBackPressed() {
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}