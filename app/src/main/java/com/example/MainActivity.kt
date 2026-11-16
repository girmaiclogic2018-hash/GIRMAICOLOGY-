package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.CoachFeedbackService
import com.example.ai.configureAppCheck
import com.example.data.LinguaDatabase
import com.example.data.LinguaRepository
import com.example.data.UserPreferencesRepository
import com.example.model.TargetLanguage
import com.example.model.ThemePreference
import com.example.ui.screens.BridgeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.LiveSessionScreen
import com.example.ui.screens.PhrasebookScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.GirmaicologyTheme
import com.example.ui.viewmodel.BridgeViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.PhrasebookViewModel
import com.example.ui.viewmodel.StudioViewModel

const val ROUTE_STUDIO = "studio"
const val ROUTE_LANGUAGE_SELECTION = "language_selection"
const val ROUTE_BRIDGE = "bridge"
const val ROUTE_PHRASEBOOK = "phrasebook"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

private data class NavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val AppTabs = listOf(
    NavTab(
        route = ROUTE_STUDIO,
        labelResId = R.string.nav_studio,
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
        testTag = "nav_tab_studio"
    ),
    NavTab(
        route = ROUTE_BRIDGE,
        labelResId = R.string.nav_bridge,
        selectedIcon = Icons.Filled.Translate,
        unselectedIcon = Icons.Outlined.Translate,
        testTag = "nav_tab_bridge"
    ),
    NavTab(
        route = ROUTE_PHRASEBOOK,
        labelResId = R.string.nav_phrasebook,
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book,
        testTag = "nav_tab_phrasebook"
    ),
    NavTab(
        route = ROUTE_HISTORY,
        labelResId = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    ),
    NavTab(
        route = ROUTE_SETTINGS,
        labelResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            GirmaicologyApp()
        }
    }
}

@Composable
fun GirmaicologyApp() {
    val studioViewModel: StudioViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = LinguaDatabase.getInstance(app)
                val repo = LinguaRepository(db.linguaDao())
                val prefs = UserPreferencesRepository(app)
                StudioViewModel(app, repo, prefs, CoachFeedbackService())
            }
        }
    )

    val bridgeViewModel: BridgeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = LinguaDatabase.getInstance(app)
                val repo = LinguaRepository(db.linguaDao())
                val prefs = UserPreferencesRepository(app)
                BridgeViewModel(app, repo, prefs, CoachFeedbackService())
            }
        }
    )

    val phrasebookViewModel: PhrasebookViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = LinguaDatabase.getInstance(app)
                val repo = LinguaRepository(db.linguaDao())
                val prefs = UserPreferencesRepository(app)
                PhrasebookViewModel(repo, prefs, CoachFeedbackService())
            }
        }
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = LinguaDatabase.getInstance(app)
                HistoryViewModel(LinguaRepository(db.linguaDao()))
            }
        }
    )

    val preferences by studioViewModel.preferences.collectAsStateWithLifecycle()
    val stats by studioViewModel.stats.collectAsStateWithLifecycle()
    val selectedScenario by studioViewModel.selectedScenario.collectAsStateWithLifecycle()
    val isSessionVisible by studioViewModel.isSessionVisible.collectAsStateWithLifecycle()
    val liveStatus by studioViewModel.liveStatus.collectAsStateWithLifecycle()
    val transcripts by studioViewModel.transcripts.collectAsStateWithLifecycle()
    val elapsedSeconds by studioViewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val reviewState by studioViewModel.reviewState.collectAsStateWithLifecycle()
    val snackbarMessage by studioViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val bridgeSourceLang by bridgeViewModel.sourceLanguage.collectAsStateWithLifecycle()
    val bridgeTone by bridgeViewModel.selectedTone.collectAsStateWithLifecycle()
    val bridgeInputText by bridgeViewModel.inputText.collectAsStateWithLifecycle()
    val bridgeTranslationState by bridgeViewModel.translationState.collectAsStateWithLifecycle()
    val bridgeWritingDraft by bridgeViewModel.writingDraft.collectAsStateWithLifecycle()
    val bridgeWritingState by bridgeViewModel.writingState.collectAsStateWithLifecycle()
    val bridgeSavedTranslations by bridgeViewModel.filteredTranslations.collectAsStateWithLifecycle()
    val bridgeAllTranslations by bridgeViewModel.allTranslations.collectAsStateWithLifecycle()
    val bridgeVaultQuery by bridgeViewModel.vaultSearchQuery.collectAsStateWithLifecycle()
    val bridgeVaultMode by bridgeViewModel.vaultFilterMode.collectAsStateWithLifecycle()
    val bridgeVaultLang by bridgeViewModel.vaultLanguageFilter.collectAsStateWithLifecycle()
    val bridgeSelectedTopic by bridgeViewModel.selectedTopic.collectAsStateWithLifecycle()
    val bridgeDrillState by bridgeViewModel.drillState.collectAsStateWithLifecycle()
    val bridgeStatusMessage by bridgeViewModel.statusMessage.collectAsStateWithLifecycle()

    val phrases by phrasebookViewModel.filteredPhrases.collectAsStateWithLifecycle()
    val availablePhraseLanguages by phrasebookViewModel.availableLanguages.collectAsStateWithLifecycle()
    val searchQuery by phrasebookViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLanguageFilter by phrasebookViewModel.selectedLanguageFilter.collectAsStateWithLifecycle()
    val masteryFilter by phrasebookViewModel.masteryFilter.collectAsStateWithLifecycle()
    val insightState by phrasebookViewModel.insightState.collectAsStateWithLifecycle()

    val sessions by historyViewModel.filteredSessions.collectAsStateWithLifecycle()
    val historyProgress by historyViewModel.progressStats.collectAsStateWithLifecycle()
    val historySearchQuery by historyViewModel.searchQuery.collectAsStateWithLifecycle()
    val historyLanguageFilter by historyViewModel.selectedLanguageFilter.collectAsStateWithLifecycle()

    val darkTheme = when (preferences.themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            studioViewModel.clearSnackbar()
        }
    }
    LaunchedEffect(bridgeStatusMessage) {
        bridgeStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            bridgeViewModel.clearStatusMessage()
        }
    }

    GirmaicologyTheme(darkTheme = darkTheme) {
        if (isSessionVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                LiveSessionScreen(
                    language = preferences.targetLanguage,
                    voice = preferences.partnerVoice,
                    scenario = selectedScenario,
                    status = liveStatus,
                    transcripts = transcripts,
                    elapsedSeconds = elapsedSeconds,
                    onTogglePause = studioViewModel::togglePause,
                    onSendText = studioViewModel::sendTextMessage,
                    onBookmarkEntry = studioViewModel::bookmarkTranscriptEntry,
                    onRetryConnect = { studioViewModel.startLiveSession(selectedScenario) },
                    onEndSession = studioViewModel::endLiveSession
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp)
                )
            }
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showAppChrome = currentDestination?.route != ROUTE_LANGUAGE_SELECTION

            val navigateToTab: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail && showAppChrome) {
                            NavigationBar {
                                AppTabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == tab.route
                                    } == true
                                    val label = stringResource(tab.labelResId)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .let { mod ->
                                if (showAppChrome) mod.padding(innerPadding) else mod
                            }
                    ) {
                        if (useNavigationRail && showAppChrome) {
                            NavigationRail {
                                AppTabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == tab.route
                                    } == true
                                    val label = stringResource(tab.labelResId)
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_STUDIO,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(ROUTE_STUDIO) {
                                StudioScreen(
                                    preferences = preferences,
                                    stats = stats,
                                    selectedScenario = selectedScenario,
                                    reviewState = reviewState,
                                    onSelectLanguage = studioViewModel::selectLanguage,
                                    onSelectLevel = studioViewModel::selectLevel,
                                    onSelectVoice = studioViewModel::selectVoice,
                                    onSelectScenario = studioViewModel::selectScenario,
                                    onOpenLanguageSelection = {
                                        navController.navigate(ROUTE_LANGUAGE_SELECTION) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onSaveSuggestedPhrase = studioViewModel::saveSuggestedPhrase,
                                    onDismissReview = studioViewModel::dismissReview
                                )
                            }

                            composable(ROUTE_LANGUAGE_SELECTION) {
                                LanguageSelectionScreen(
                                    selectedLanguage = preferences.targetLanguage,
                                    selectedScenario = selectedScenario,
                                    proficiencyLevel = preferences.proficiencyLevel,
                                    hasAudioPermission = studioViewModel::hasAudioPermission,
                                    onSelectLanguage = studioViewModel::selectLanguage,
                                    onStartLiveSession = {
                                        navController.popBackStack()
                                        studioViewModel.startLiveSession(selectedScenario)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_BRIDGE) {
                                val appContext = androidx.compose.ui.platform.LocalContext.current
                                BridgeScreen(
                                    preferences = preferences,
                                    sourceLanguage = bridgeSourceLang,
                                    selectedTone = bridgeTone,
                                    inputText = bridgeInputText,
                                    translationState = bridgeTranslationState,
                                    writingDraft = bridgeWritingDraft,
                                    writingState = bridgeWritingState,
                                    savedTranslations = bridgeSavedTranslations,
                                    allTranslationsCount = bridgeAllTranslations.size,
                                    vaultSearchQuery = bridgeVaultQuery,
                                    vaultFilterMode = bridgeVaultMode,
                                    vaultLanguageFilter = bridgeVaultLang,
                                    selectedTopic = bridgeSelectedTopic,
                                    drillState = bridgeDrillState,
                                    onSourceLanguageChange = bridgeViewModel::setSourceLanguage,
                                    onTargetLanguageChange = bridgeViewModel::setTargetLanguage,
                                    onSelectTone = bridgeViewModel::setTranslationTone,
                                    onSwapLanguages = bridgeViewModel::swapLanguages,
                                    onInputTextChange = bridgeViewModel::setInputText,
                                    onTranslate = bridgeViewModel::translateNow,
                                    onUploadImageUri = { uri ->
                                        bridgeViewModel.translateImageUri(appContext, uri)
                                    },
                                    onUploadDocumentUri = { uri ->
                                        bridgeViewModel.translateDocumentUri(appContext, uri)
                                    },
                                    onToggleStarCurrent = bridgeViewModel::toggleStarCurrentTranslation,
                                    onEditCurrentTranslation = bridgeViewModel::editCurrentTranslation,
                                    onSaveTranslation = bridgeViewModel::saveTranslationToPhrasebook,
                                    onWritingDraftChange = bridgeViewModel::setWritingDraft,
                                    onPolishWriting = bridgeViewModel::polishWritingNow,
                                    onSaveWritingToVault = bridgeViewModel::saveWritingToVault,
                                    onSaveWritingToPhrasebook = bridgeViewModel::saveWritingToPhrasebook,
                                    onVaultSearchQueryChange = bridgeViewModel::setVaultSearchQuery,
                                    onVaultFilterModeChange = bridgeViewModel::setVaultFilterMode,
                                    onVaultLanguageFilterChange = bridgeViewModel::setVaultLanguageFilter,
                                    onClearVaultFilters = bridgeViewModel::clearVaultFilters,
                                    onToggleStarRecord = bridgeViewModel::toggleStarRecord,
                                    onUpdateSavedRecord = bridgeViewModel::updateSavedRecord,
                                    onDeleteSavedRecord = bridgeViewModel::deleteSavedRecord,
                                    onSaveRecordToPhrasebook = bridgeViewModel::saveRecordToPhrasebook,
                                    onLoadRecordIntoTranslator = bridgeViewModel::loadRecordIntoTranslator,
                                    onSpeakText = bridgeViewModel::speakText,
                                    onShowMessage = bridgeViewModel::showStatusMessage,
                                    onSelectTopic = bridgeViewModel::setDrillTopic,
                                    onGenerateDrill = bridgeViewModel::generateDrill,
                                    onSelectAnswer = bridgeViewModel::selectAnswer,
                                    onSaveDrillExpression = bridgeViewModel::saveDrillExpression,
                                    onPracticeInLiveVoice = { lang ->
                                        studioViewModel.selectLanguage(lang)
                                        navController.navigate(ROUTE_LANGUAGE_SELECTION) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_PHRASEBOOK) {
                                PhrasebookScreen(
                                    phrases = phrases,
                                    availableLanguages = availablePhraseLanguages,
                                    searchQuery = searchQuery,
                                    selectedLanguageFilter = selectedLanguageFilter,
                                    masteryFilter = masteryFilter,
                                    defaultLanguage = preferences.targetLanguage,
                                    insightState = insightState,
                                    onSearchQueryChange = phrasebookViewModel::setSearchQuery,
                                    onLanguageFilterChange = phrasebookViewModel::setLanguageFilter,
                                    onMasteryFilterChange = phrasebookViewModel::setMasteryFilter,
                                    onAddPhrase = phrasebookViewModel::addManualPhrase,
                                    onUpdatePhrase = phrasebookViewModel::updatePhrase,
                                    onMarkReviewed = phrasebookViewModel::markReviewed,
                                    onDeletePhrase = phrasebookViewModel::deletePhrase,
                                    onExplainPhrase = phrasebookViewModel::requestPhraseInsight,
                                    onSpeakPhrase = { item ->
                                        val matchedLang = TargetLanguage.entries.firstOrNull {
                                            it.code == item.languageCode
                                        } ?: preferences.targetLanguage
                                        bridgeViewModel.speakText(item.phrase, matchedLang, slowRate = true)
                                    },
                                    onShowMessage = bridgeViewModel::showStatusMessage,
                                    onClearFilters = phrasebookViewModel::clearFilters,
                                    onDismissInsight = phrasebookViewModel::dismissInsight
                                )
                            }

                            composable(ROUTE_HISTORY) {
                                HistoryScreen(
                                    sessions = sessions,
                                    progressStats = historyProgress,
                                    searchQuery = historySearchQuery,
                                    selectedLanguageFilter = historyLanguageFilter,
                                    onSearchQueryChange = historyViewModel::setSearchQuery,
                                    onLanguageFilterChange = historyViewModel::setLanguageFilter,
                                    onPracticeAgain = { session ->
                                        studioViewModel.launchPracticeFor(
                                            languageCode = session.languageCode,
                                            scenarioTitle = session.scenarioTitle
                                        )
                                        navigateToTab(ROUTE_STUDIO)
                                        navController.navigate(ROUTE_LANGUAGE_SELECTION) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onDeleteSession = historyViewModel::deleteSession
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    preferences = preferences,
                                    onNativeLanguageChange = studioViewModel::setNativeLanguage,
                                    onGentleCorrectionsChange = studioViewModel::setGentleCorrections,
                                    onDailyGoalChange = studioViewModel::setDailyGoalMinutes,
                                    onThemeChange = studioViewModel::setThemePreference,
                                    onClearAllData = studioViewModel::clearAllLocalData
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
