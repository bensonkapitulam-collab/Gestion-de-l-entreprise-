package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PasswordStore
import com.example.data.ShopDatabase
import com.example.data.ShopRepository
import com.example.ui.ShopTab
import com.example.ui.ShopViewModel
import com.example.ui.ShopViewModelFactory
import com.example.ui.screens.ArrivageScreen
import com.example.ui.screens.FactureScreen
import com.example.ui.screens.HonneurScreen
import com.example.ui.screens.JourScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MoisScreen
import com.example.ui.screens.SemaineScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.GestionBoutiqueTheme
import com.example.util.ReportExporter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ShopDatabase.getInstance(applicationContext)
        val passwordStore = PasswordStore(applicationContext)
        val repository = ShopRepository(db.shopDao(), passwordStore)

        setContent {
            GestionBoutiqueTheme {
                val vm: ShopViewModel = viewModel(factory = ShopViewModelFactory(repository))
                val state by vm.uiState.collectAsStateWithLifecycle()

                if (state.isLocked) {
                    LockScreen(
                        hasPassword = state.hasPassword,
                        error = state.lockError,
                        onCreatePassword = vm::createPassword,
                        onUnlock = vm::unlock
                    )
                } else {
                    MainShopApp(vm = vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShopApp(vm: ShopViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = state.selectedTab != ShopTab.FACTURE) {
        vm.selectTab(ShopTab.FACTURE)
    }

    val handlePrint: (ShopTab) -> Unit = { tab ->
        val report = vm.buildReportForTab(tab)
        val bmp = ReportExporter.renderReportBitmap(report)
        ReportExporter.printBitmap(context, report.title, bmp)
    }

    val handleExportJpeg: (ShopTab) -> Unit = { tab ->
        val report = vm.buildReportForTab(tab)
        val bmp = ReportExporter.renderReportBitmap(report)
        val (savedToGallery, shareUri) = ReportExporter.saveJpegToGalleryAndCache(context, report.title, bmp)
        scope.launch {
            val msg = if (savedToGallery) {
                "Image enregistrée dans Pictures/GestionBoutique"
            } else {
                "Image prête à partager"
            }
            val res = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (shareUri != null) "Partager" else null,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed && shareUri != null) {
                ReportExporter.shareImageUri(context, shareUri, report.title)
            }
        }
    }

    if (state.alertMessage != null) {
        AlertDialog(
            onDismissRequest = vm::dismissAlert,
            title = { Text("Attention") },
            text = { Text(state.alertMessage ?: "") },
            confirmButton = {
                TextButton(onClick = vm::dismissAlert) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.boutique_icon_1793964344900),
                            contentDescription = "Logo Gestion Boutique",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gestion Boutique",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = vm::lockApp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("lock_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verrouiller")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShopTab.entries) { tab ->
                    val selected = tab == state.selectedTab
                    FilterChip(
                        selected = selected,
                        onClick = { vm.selectTab(tab) },
                        label = { Text(tab.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = tabIcon(tab),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .padding(bottom = 28.dp)
                ) {
                    when (state.selectedTab) {
                        ShopTab.FACTURE -> FactureScreen(
                            state = state,
                            onClientNameChange = vm::updateClientName,
                            onUpdateLine = vm::updateCartLine,
                            onPickProduct = vm::pickProductForCartLine,
                            onAddLine = vm::addCartLine,
                            onRemoveLine = vm::removeCartLine,
                            onCancelEdit = vm::cancelEditInvoice,
                            onValidateInvoice = vm::validateAndSaveInvoice,
                            onEditInvoice = vm::startEditInvoice,
                            onPrintInvoice = { handlePrint(ShopTab.FACTURE) },
                            onExportInvoiceJpeg = { handleExportJpeg(ShopTab.FACTURE) }
                        )
                        ShopTab.ARRIVAGE -> ArrivageScreen(
                            state = state,
                            onUpdateLine = vm::updateArrivalLine,
                            onAddLine = vm::addArrivalLine,
                            onRemoveLine = vm::removeArrivalLine,
                            onSaveArrivals = vm::saveArrivals
                        )
                        ShopTab.STOCK -> StockScreen(
                            state = state,
                            onPrint = { handlePrint(ShopTab.STOCK) },
                            onExportJpeg = { handleExportJpeg(ShopTab.STOCK) }
                        )
                        ShopTab.HONNEUR -> HonneurScreen(state = state)
                        ShopTab.JOUR -> JourScreen(
                            state = state,
                            onShiftDate = vm::shiftSelectedDate,
                            onPrint = { handlePrint(ShopTab.JOUR) },
                            onExportJpeg = { handleExportJpeg(ShopTab.JOUR) }
                        )
                        ShopTab.SEMAINE -> SemaineScreen(
                            state = state,
                            onPrint = { handlePrint(ShopTab.SEMAINE) },
                            onExportJpeg = { handleExportJpeg(ShopTab.SEMAINE) }
                        )
                        ShopTab.MOIS -> MoisScreen(
                            state = state,
                            onShiftMonth = vm::shiftSelectedMonth,
                            onPrint = { handlePrint(ShopTab.MOIS) },
                            onExportJpeg = { handleExportJpeg(ShopTab.MOIS) }
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: ShopTab) = when (tab) {
    ShopTab.FACTURE -> Icons.Default.ReceiptLong
    ShopTab.ARRIVAGE -> Icons.Default.Warehouse
    ShopTab.STOCK -> Icons.Default.Inventory2
    ShopTab.HONNEUR -> Icons.Default.EmojiEvents
    ShopTab.JOUR -> Icons.Default.CalendarToday
    ShopTab.SEMAINE -> Icons.Default.ShowChart
    ShopTab.MOIS -> Icons.Default.CalendarMonth
}
