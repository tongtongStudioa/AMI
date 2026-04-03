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
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.MaterialFadeThrough
import com.google.gson.Gson
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.databinding.FragmentSettingsBinding
import com.tongtongstudio.ami.domain.usecase.DatabaseBackupDto
import com.tongtongstudio.ami.domain.usecase.ExportDatabaseUseCase
import com.tongtongstudio.ami.domain.usecase.ImportDatabaseUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) { // TODO: update this fragment only with ui modification and remove logic elsewhere

    @Inject lateinit var importDatabaseUseCase: ImportDatabaseUseCase

    @Inject lateinit var exportDatabaseUseCase: ExportDatabaseUseCase

    private lateinit var binding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSettingsBinding.bind(view)

        setUpToolbar()



        binding.saveButton.setOnClickListener {
            // Ask user to edit file name
            showSaveFileDialog()
        }

        binding.importButton.setOnClickListener {
            // Open file explorer to import a file
            openFilePicker()
        }

        binding.deleteDataButton.setOnClickListener {
            suppressAllData()
        }

    }

    private fun suppressAllData() {
        //TODO("Not yet implemented")
        Toast.makeText(
            requireContext(),
            "Not yet implemented !",
            Toast.LENGTH_SHORT
        ).show()
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
            val json = withContext(Dispatchers.IO) {
                exportDatabaseUseCase()
            }
            saveToFile(fileName, json)
        }
    }

    private fun saveToFile(fileName: String, databaseJson: String) {
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
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    importDatabaseUseCase(json)
                }

                result.fold(
                    onSuccess = {
                        Toast.makeText(requireContext(), "Import réussi", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
            /*val databaseMapType = object : TypeToken<Map<String, List<Any>>>() {}.type
            val dataMap: Map<String, List<Any>> = Gson().fromJson(json, databaseMapType)

            // Insérer les données dans les tables respectives
            lifecycleScope.launch(Dispatchers.IO) {
                var error = false
                try {
                    taskDao.insertTasks(dataMap["tasks"] as List<Task>)
                    workSessionDao.insertWorkSessions(dataMap["work_sessions"] as List<WorkSession>)
                    reminderDao.insertReminders(dataMap["reminders"] as List<Reminder>)
                    categoryDao.insertCategories(dataMap["categories"] as List<Category>)
                    assessmentDao.insertAssessments(dataMap["assessments"] as List<Assessment>)
                    error = false
                    Toast.makeText(requireContext(), "Base de données restaurée", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Log.e("IMPORT USE CASE", e.message ?: "No message")
                    error = true
                }
                if (error)
                    Toast.makeText(requireContext(), "Problème lors de l'importation !", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(requireContext(), "Base de données restaurée", Toast.LENGTH_SHORT).show()


            }*/
        }
    }

    companion object {
        private const val REQUEST_CODE_IMPORT = 1001
    }

    // function to set up toolbar with collapse toolbar and link to drawer layout
    private fun setUpToolbar() {
        val mainActivity = activity as MainActivity
        // imperative to see option menu and navigation icon (hamburger)
        mainActivity.setSupportActionBar(binding.toolbar)

        val navController = findNavController()
        // retrieve app bar configuration : see MainActivity.class
        val appBarConfiguration = mainActivity.appBarConfiguration

        // to set hamburger menu work and open drawer layout
        binding.toolbar.setupWithNavController(navController, appBarConfiguration).apply {
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
        }
    }
}
