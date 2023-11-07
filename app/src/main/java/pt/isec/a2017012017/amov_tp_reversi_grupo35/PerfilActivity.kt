package pt.isec.a2017012017.amov_tp_reversi_grupo35

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_perfil.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val REQUEST_IMAGE_CAPTURE = 1


class PerfilActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    var imageUri: Uri? = null
    var ultimo_modo: Int = 0
    var btn1v1: Boolean = false
    var btn1v1v1: Boolean = true
    lateinit var storageReference:StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)
        auth = FirebaseAuth.getInstance()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        checkProfile()
        updateTopScore(2)
        updateBotoes()

        ibtn_mudar_imagem.setOnClickListener {

               // hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent,REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.perfil_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.it_gravar -> {
                updateProfile()
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




//guardar foto
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            iv_imagem_perfil.setImageBitmap(imageBitmap)

            //passar Bitmap para Uri

            val bytes = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(applicationContext.contentResolver, imageBitmap, "Title", null)
            imageUri = Uri.parse(path.toString())
        }
    }



    private fun checkProfile(){
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, R.string.ainda_nao_esta_logadp,Toast.LENGTH_SHORT ).show()
            val myIntent = Intent(this, LoginActivity::class.java)
            startActivity(myIntent)
            finish()
        }else{

            storageReference = FirebaseStorage.getInstance().reference.child("Users/"+user.uid+".jpg")
            val localfile = File.createTempFile("temporary","jpg")
            storageReference.getFile(localfile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                iv_imagem_perfil.setImageBitmap(bitmap)

            }

            et_nomePerfil.setText(user.displayName)
            et_email.setText(user.email)
        }

    }



    private fun updateTopScore(modo: Int) {
        val db = Firebase.firestore
        val username = auth.currentUser!!.displayName
        val userid = auth.currentUser!!.uid
        ultimo_modo = modo

        var collection = db.collection("Users").document(userid)
            .collection("Scores_modo$modo")

        //percorrer todos os resultados
        for (i in 1..5) {
            collection.document("game$i")
                .addSnapshotListener { docSS, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    if (docSS != null && docSS.exists()) {                    //se existe documentos
                        when (i) {
                            1-> {   if (modo == 2)
                                        findViewById<TextView>(R.id.txt_S1).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv").toString()
                                    else
                                        findViewById<TextView>(R.id.txt_S1).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv1").toString()+"\n" + docSS.getString("adv2").toString()
                            }
                            2->  {   if (modo == 2)
                                findViewById<TextView>(R.id.txt_S2).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv").toString()
                            else
                                findViewById<TextView>(R.id.txt_S2).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv1").toString()+"\n" + docSS.getString("adv2").toString()
                            }
                            3-> {   if (modo == 2)
                                findViewById<TextView>(R.id.txt_S3).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv").toString()
                            else
                                findViewById<TextView>(R.id.txt_S3).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv1").toString()+"\n" + docSS.getString("adv2").toString()
                            }
                            4-> {   if (modo == 2)
                                findViewById<TextView>(R.id.txt_S4).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv").toString()
                            else
                                findViewById<TextView>(R.id.txt_S4).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv1").toString()+"\n" + docSS.getString("adv2").toString()
                            }
                            5->  {   if (modo == 2)
                                findViewById<TextView>(R.id.txt_S5).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv").toString()
                            else
                                findViewById<TextView>(R.id.txt_S5).text = username + "\n" + docSS.getString("S").toString() + "\n" + docSS.getString("adv1").toString()+"\n" + docSS.getString("adv2").toString()
                            }
                        }
                    }else
                        Toast.makeText(this, "Error Updating Top Score",Toast.LENGTH_SHORT).show()
                }
        }
    }




    private fun updateProfile(){
        val user = auth.currentUser
        val profileUpdates : UserProfileChangeRequest
        user?.let { user ->
            val username = et_nomePerfil.text.toString()
            profileUpdates =
                UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
            if(imageUri!=null) {
                val photoURI = Uri.parse(imageUri.toString())
                storageReference = FirebaseStorage.getInstance().getReference("Users/"+user.uid+".jpg")
                storageReference.putFile(photoURI).addOnSuccessListener {
                    Toast.makeText(this@PerfilActivity, "Successfully updated profile picture",
                        Toast.LENGTH_LONG).show()
                }.addOnFailureListener{
                    Toast.makeText(this@PerfilActivity, "Failed updating profile picture",
                        Toast.LENGTH_LONG).show()
                }

            }


            CoroutineScope(Dispatchers.IO).launch {
                try {
                    user.updateProfile(profileUpdates).await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PerfilActivity, "Successfully updated profile",
                            Toast.LENGTH_LONG).show()
                        updateTopScore(ultimo_modo)
                    }
                } catch(e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PerfilActivity, e.message, Toast.LENGTH_LONG).show()
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



    fun onClick_btn1v1(view: View){
        btn1v1 = false
        btn1v1v1 = true
        updateBotoes()

        updateTopScore(2)
    }

    fun onClick_btn1v1v1(view: View){
        btn1v1 = true
        btn1v1v1 = false
        updateBotoes()

        updateTopScore(3)
    }


    private fun updateBotoes() {
        if (!btn1v1){
            findViewById<Button>(R.id.btn_1v1).setBackgroundColor(resources.getColor(R.color.grey))
            findViewById<Button>(R.id.btn_1v1).isClickable = false
        }else{
            findViewById<Button>(R.id.btn_1v1).setBackgroundColor(resources.getColor(R.color.dark_green))
            findViewById<Button>(R.id.btn_1v1).isClickable = true
        }

        if (!btn1v1v1){
            findViewById<Button>(R.id.btn_1v1v1).setBackgroundColor(resources.getColor(R.color.grey))
            findViewById<Button>(R.id.btn_1v1v1).isClickable = false
        }else{
            findViewById<Button>(R.id.btn_1v1v1).setBackgroundColor(resources.getColor(R.color.dark_green))
            findViewById<Button>(R.id.btn_1v1v1).isClickable = true
        }

    }



}
