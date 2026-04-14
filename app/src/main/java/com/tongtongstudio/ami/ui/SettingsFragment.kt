package com.tongtongstudio.ami.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentSettingsBinding
import com.tongtongstudio.ami.domain.usecase.ExportDatabaseUseCase
import com.tongtongstudio.ami.domain.usecase.ImportDatabaseUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment :
    Fragment(R.layout.fragment_settings) { // TODO: update this fragment only with ui modification and remove logic elsewhere

    @Inject
    lateinit var importDatabaseUseCase: ImportDatabaseUseCase

    @Inject
    lateinit var exportDatabaseUseCase: ExportDatabaseUseCase

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

    // Show dialog to enter name for save file
    private fun showSaveFileDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setTitle(getString(R.string.save_file_name))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    saveDatabaseToJson(fileName)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_valid_name), Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Save database in json file
    private fun saveDatabaseToJson(fileName: String) {
        lifecycleScope.launch {
            val json = withContext(Dispatchers.IO) {
                exportDatabaseUseCase()
            }
            saveToFile(fileName, json)
        }
    }

    private fun saveToFile(fileName: String, databaseJson: String) {
        val file = File(requireContext().filesDir, "$fileName.json")
        file.writeText(databaseJson)
        val exportPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$fileName.json"
        )

        try {
            file.copyTo(exportPath, overwrite = true)
        } catch (e: IOException) {
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.export_success_msg, fileName),
            Toast.LENGTH_SHORT
        ).show()
    }


    // Open file explorer to select json file
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_IMPORT)
    }

    // Retrieve json file after export
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                importDatabaseFromJson(uri)
            }
        }
    }

    private fun importDatabaseFromJson(uri: Uri) {

        requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            val json = it.readText()
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    importDatabaseUseCase(json)
                }

                result.fold(
                    onSuccess = {
                        Toast.makeText(requireContext(),
                            getString(R.string.import_success_msg), Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.import_failure_msg, error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
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
