package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        tv_login_registar.setOnClickListener {
            startActivity(Intent(this,RegisterActivity::class.java))
        }

        tv_esqueceupass.setOnClickListener {
            startActivity(Intent(this,EsqueceuPassActivity::class.java))
        }

        ed_login_email.setText("andre.pessoa.coelho@gmail.com")
        ed_login_password.setText("password")


        btn_login.setOnClickListener{
            when {
                //se estiver em branco
                TextUtils.isEmpty((ed_login_email).text.toString().trim{it<= ' '}) -> {
                    Toast.makeText(this, R.string.porfavor_escreva_email, Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty((ed_login_password).text.toString().trim{it<= ' '}) -> {
                    Toast.makeText(this, R.string.porfavor_escreva_password, Toast.LENGTH_SHORT).show()
                }
                else -> {

                    val email: String = ed_login_email.text.toString().trim{it<= ' '}
                    val password: String = ed_login_password.text.toString().trim{it<= ' '}

                    //Log-In com firebase
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->

                                //se o login foi bem sucedido
                                if(task.isSuccessful) {
                                    val firebaseUser: FirebaseUser = task.result!!.user!!

                                    Toast.makeText(this, R.string.login_sucesso, Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this, MainActivity::class.java)
                                    //limpar a pilha de activities
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    intent.putExtra("user_id", FirebaseAuth.getInstance().currentUser!!.uid)
                                    intent.putExtra("username", FirebaseAuth.getInstance().currentUser!!.displayName)
                                    intent.putExtra("email_id", email)
                                    startActivity(intent)
                                    finish()
                                }else {
                                    Toast.makeText(this, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }
                }
            }


        }

    }

}