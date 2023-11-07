package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val db = Firebase.firestore
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btn_register.setOnClickListener{
            when {
                //se estiver em branco
                TextUtils.isEmpty((ed_register_username).text.toString().trim{it<= ' '}) -> {
                    Toast.makeText(this, R.string.porfavor_escreva_username, Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty((ed_register_email).text.toString().trim{it<= ' '}) -> {
                    Toast.makeText(this, R.string.porfavor_escreva_email, Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty((ed_register_password).text.toString().trim{it<= ' '}) -> {
                    Toast.makeText(this, R.string.porfavor_escreva_password, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val username: String = ed_register_username.text.toString().trim{it<= ' '}
                    val email: String = ed_register_email.text.toString().trim{it<= ' '}
                    val password: String = ed_register_password.text.toString().trim{it<= ' '}

                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(
                            OnCompleteListener<AuthResult>{task ->

                                //se o registar foi bem sucedido
                                if(task.isSuccessful) {
                                    val firebaseUser: FirebaseUser = task.result!!.user!!

                                    val updateUsername = UserProfileChangeRequest.Builder()
                                        .setDisplayName(username).build()
                                    FirebaseAuth.getInstance().currentUser?.updateProfile(updateUsername)           //guardar username

                                    Toast.makeText(
                                        this,
                                        R.string.registado_sucesso,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    var fAuth = FirebaseAuth.getInstance()

                                    /**criar bd*/
                                    var userid = fAuth.currentUser?.uid
                                    
                                    val game_mode2 = hashMapOf(     //{score, username_adversario}
                                        "S" to "0:0",
                                        "adv" to " "
                                    )

                                    val game_mode3 = hashMapOf(     //{score, username_adversario}
                                        "S" to "0:0:0",
                                        "adv1" to " ",
                                        "adv2" to " "
                                    )


                                    if (userid != null) {//ver exceções maybe
                                        for (i in 1..5 ) {
                                            db.collection("Users").document(userid)
                                                .collection("Scores_modo2").document("game$i")
                                                .set(game_mode2)
                                            db.collection("Users").document(userid)
                                                .collection("Scores_modo3").document("game$i")
                                                .set(game_mode3)
                                        }
                                    }



                                    val intent = Intent(this, MainActivity::class.java)
                                    //limpar a pilha de activities
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    intent.putExtra("user_id", firebaseUser.uid)
                                    intent.putExtra("username", firebaseUser.displayName)
                                    intent.putExtra("email_id", email)
                                    startActivity(intent)
                                    finish()
                                }else {
                                    Toast.makeText(this, task.exception!!.message.toString(),Toast.LENGTH_SHORT).show()
                                }
                        })
                }
            }


            }

        tv_jatemconta.setOnClickListener {
            finish()
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