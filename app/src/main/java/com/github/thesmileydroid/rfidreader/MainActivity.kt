@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.thesmileydroid.rfidreader

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.thesmileydroid.rfidreader.ui.theme.RFIDReaderTheme
import kotlin.reflect.KProperty

class TagViewModel : ViewModel() {
    private val _tagList = MutableLiveData<List<Tag>>()
    val tagList: LiveData<List<Tag>> = _tagList

    fun addTag(tag: Tag) {
        _tagList.value = (_tagList.value ?: emptyList()) + tag
    }


    operator fun getValue(mainActivity: MainActivity, property: KProperty<*>): TagViewModel {
        return this
    }
}

class LogViewModel : ViewModel() {
    private val _logList = MutableLiveData<List<String>>()
    val logList: LiveData<List<String>> = _logList

    @Composable
    fun addLog(log: String) {
        _logList.value = (_logList.value ?: emptyList()) + log
        Log.d("nfcView", log)
    }

    operator fun getValue(mainActivity: MainActivity, property: KProperty<*>): LogViewModel {
        return this
    }
}

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    private val tagViewModel: TagViewModel by TagViewModel()
    private val logViewModel: LogViewModel by LogViewModel()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RFIDReaderTheme {
                Column(modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())) {
                    TopAppBar(title = {
                        Text(text = "RFID Reader")
                    })
                    Surface(modifier = Modifier.fillMaxSize()) {
                        TagIndicator(viewModel = tagViewModel)
                    }
                }
            }
        }

        try {
            // Requisitar o adaptador NFC padrão para este dispositivo
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)

            // Criar um intent para quando o NFC for descoberto
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_MUTABLE)
            if (nfcAdapter != null) {
                // Habilitar o NFC
                nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
            }
        } catch (e: Exception) {
            Log.e("nfc", e.toString())
        }
    }

    // Quando a atividade for retomada, definir o intent para o adaptador NFC
    override fun onResume() {
        super.onResume()
        // Criar um intent para quando o NFC for descoberto
        if (pendingIntent == null) {
            pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }, PendingIntent.FLAG_MUTABLE)
        }
        // Habilitar o NFC
        if (nfcAdapter != null) {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    // Quando a atividade for pausada, desabilitar o NFC
    override fun onPause() {
        super.onPause()
        if (nfcAdapter != null) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
    }

    @Composable
    private fun logToUI(message: String) {
        logViewModel.addLog(message)
    }

    // Quando um novo intent for recebido (quando o NFC for descoberto)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("nfc", intent.toString())
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Obter o tag descoberto
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            tag?.let { tagViewModel.addTag(it) }
            Log.d("nfc", tag.toString())
            val tagId = tag.id
            Log.d("nfc", tagId.toString())
            val tagTechList = tag.techList
            Log.d("nfc", tagTechList.toString())
            val tagTech = tagTechList?.get(0)
            Log.d("nfc", tagTech.toString())
        }
    }
}



@Composable
fun TagIndicator(viewModel: TagViewModel, lifecycleOwner: LifecycleEventObserver? = null) {
    val text = remember {mutableStateOf("Waiting for tag...")} // Esperando por tag...
    viewModel.tagList.observe(LocalLifecycleOwner.current) { // Observar a lista de tags
        Log.d("nfcView", "tagList changed")
        val lastTag = it.lastOrNull() // Obter a última tag encontrada
        if (it == null || it.isEmpty()) {
            text.value = "Waiting for tag..."
        } else {
            var result = MifareTagTester().getUUID(lastTag!!) + "\n" // Obter o UUID da tag
            result += "\n"
            for (tech in lastTag.techList!!) { // Obter a lista de tecnologias da tag
                result += "Has tech: $tech\n" // Tem tecnologia X
            }
            text.value = result // Atualizar o texto
            try { // Tentar ler/escrever na tag
                result += MifareTagTester().getInfo(lastTag) + "\n" // Obter informações da tag
                Log.d("nfc", text.toString())

                result += MifareTagTester().readTag(lastTag) + "\n" // Ler a tag
                Log.d("nfc", text.toString())

                result += MifareTagTester().writeTag(lastTag, "Projeto de Redes", 2) + "\n" // Escrever na tag
                Log.d("nfc", text.toString())
            } catch (e: Exception) {
                Log.e("nfc", e.toString())
                result += "Error: $e"
            }

            text.value = result // Atualizar o texto
        }
    }
    if (viewModel.tagList.value == null || viewModel.tagList.value!!.isEmpty()) {
        Row {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Waiting for tag..."
            )
            Text(text.value)
        }
    } else {
        Text(text = text.value)
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RFIDReaderTheme {
        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())) {
            TopAppBar(title = {
                Text(text = "RFID Reader")
            })
            Surface(modifier = Modifier.fillMaxSize()) {
                TagIndicator(viewModel = TagViewModel())
            }
        }
    }
}