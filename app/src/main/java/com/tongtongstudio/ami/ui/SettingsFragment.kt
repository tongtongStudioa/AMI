package com.tongtongstudio.ami.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tongtongstudio.ami.Application
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.ThingToDoDatabase
import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.WorkSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var workSessionDao: WorkSessionDao

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var assessmentDao: AssessmentDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveButton = view.findViewById<Button>(R.id.save_button)
        val importButton = view.findViewById<Button>(R.id.import_button)



        saveButton.setOnClickListener {
            // Demander à l'utilisateur de choisir le nom du fichier et sauvegarder
            showSaveFileDialog()
        }

        importButton.setOnClickListener {
            // Ouvrir le file explorer pour importer un fichier
            openFilePicker()
        }

    }

    // Afficher un dialogue pour choisir le nom du fichier de sauvegarde
    private fun showSaveFileDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setTitle("Nom du fichier de sauvegarde")
            .setView(input)
            .setPositiveButton("Sauvegarder") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    saveDatabaseToJson(fileName)
                } else {
                    Toast.makeText(requireContext(), "Veuillez entrer un nom valide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Sauvegarder la base de données dans un fichier JSON
    private fun saveDatabaseToJson(fileName: String) {
        lifecycleScope.launch {
            val tasks: List<Task> = taskDao.getAllTasks().first() // Collecte de toutes les tâches
            val workSessions: List<WorkSession> =
                workSessionDao.getAllWorkSessions().first() // Collecte des work sessions
            val reminders: List<Reminder> = reminderDao.getAllReminders().first() // Collecte des rappels
            val categories: List<Category> = categoryDao.getAllCategories().first() // Collecte des catégories
            val assessments = assessmentDao.getAllAssessments().first() // Collecte des unités

            // Convertir les données collectées en JSON
            val databaseJson = Gson().toJson(
                mapOf(
                    "tasks" to tasks,
                    "work_sessions" to workSessions,
                    "reminders" to reminders,
                    "categories" to categories,
                    "units" to assessments
                )
            )

            // Sauvegarder dans le fichier
            val file = File(requireContext().filesDir, "$fileName.json")
            file.writeText(databaseJson)
            val exportPath = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "$fileName.json"
            )

            try {
                file.copyTo(exportPath, overwrite = true)
                Log.d("DB_EXPORT", "Base de données copiée avec succès vers ${exportPath.absolutePath}")
            } catch (e: IOException) {
                Log.e("DB_EXPORT", "Erreur lors de la sauvegarde", e)
            }

            Toast.makeText(
                requireContext(),
                "Base de données sauvegardée sous $fileName",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // Ouvrir un explorateur de fichiers pour importer un fichier JSON
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val task = Task("name",null, null)
        startActivityForResult(intent, REQUEST_CODE_IMPORT)
    }

    // Récupérer le fichier JSON après l'importation
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                importDatabaseFromJson(uri)
            }
        }
    }

    // Importer les données depuis un fichier JSON
    private fun importDatabaseFromJson(uri: Uri) {
        requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            val json = it.readText()
            val databaseMapType = object : TypeToken<Map<String, List<Any>>>() {}.type
            val dataMap: Map<String, List<Any>> = Gson().fromJson(json, databaseMapType)

            // Insérer les données dans les tables respectives
            lifecycleScope.launch {
                taskDao.insertTasks(dataMap["tasks"] as List<Task>)
                workSessionDao.insertWorkSessions(dataMap["work_sessions"] as List<WorkSession>)
                reminderDao.insertReminders(dataMap["reminders"] as List<Reminder>)
                categoryDao.insertCategories(dataMap["categories"] as List<Category>)
                assessmentDao.insertAssessments(dataMap["units"] as List<Assessment>)

                Toast.makeText(requireContext(), "Base de données restaurée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMPORT = 1001
    }

}
