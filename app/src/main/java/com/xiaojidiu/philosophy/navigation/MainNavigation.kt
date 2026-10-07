package com.xiaojidiu.philosophy.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.ui.AppViewModel
import com.xiaojidiu.philosophy.ui.home.HomeScreen
import com.xiaojidiu.philosophy.ui.question.*
import com.xiaojidiu.philosophy.ui.result.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.*
import androidx.lifecycle.createSavedStateHandle
import com.xiaojidiu.philosophy.ui.profile.*
import com.xiaojidiu.philosophy.ui.history.HistoryScreen
import com.xiaojidiu.philosophy.ui.explore.*
import com.xiaojidiu.philosophy.ui.philosopher.PhilosopherDetailScreen
import com.xiaojidiu.philosophy.ui.school.SchoolDetailScreen
import com.xiaojidiu.philosophy.ui.favorite.*
import com.xiaojidiu.philosophy.ui.profile.SnapshotScreen
import com.xiaojidiu.philosophy.ui.debate.*
import com.xiaojidiu.philosophy.ui.settings.*
import com.xiaojidiu.philosophy.ui.games.*
import com.xiaojidiu.philosophy.ui.music.*
import androidx.compose.ui.platform.LocalDensity

@Composable fun MainNavigation(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val library by vm.library.collectAsStateWithLifecycle()
    val journal by vm.journal.collectAsStateWithLifecycle()
    val identity by vm.graph.preferences.settings.collectAsStateWithLifecycle(com.xiaojidiu.philosophy.data.local.SettingsState())
    val games by vm.games.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    fun tab(route: String) { nav.navigate(route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }
    Scaffold(bottomBar = { Column {if(entry?.destination?.route!="bgm" && WindowInsets.ime.getBottom(LocalDensity.current)==0) BgmMiniBar(vm.graph.bgm) {nav.navigate("bgm") {launchSingleTop=true}};if(entry?.destination?.route in mainRoutes) XjdBottomBar(entry?.destination?.route,::tab)} }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("home") { HomeScreen(vm,library) { id,choice -> nav.navigate("question/$id${if(choice==null) "" else "?choice=$choice"}") } }
            composable("explore") { ExploreScreen(library,{nav.navigate("question/$it")},{nav.navigate("philosopher/$it")},{nav.navigate("school/$it")},{nav.navigate("map")},{nav.navigate("games")}) }
            composable("games") {GameWorksScreen(games,{nav.popBackStack()},{nav.navigate("game/$it")})}
            composable("game/{id}") {e->GameWorkDetailScreen(games.find {it.id==e.arguments?.getString("id")},library,{nav.popBackStack()},{nav.navigate("philosopher/$it")},{nav.navigate("question/$it")})}
            composable("debate") {
                val dvm: DebateViewModel=viewModel(factory=viewModelFactory {initializer {DebateViewModel(vm.graph,createSavedStateHandle())}})
                DebateScreen(dvm,library,journal,{vm.toggleFavorite("conversation",it)},{nav.navigate("conversations")},configure={nav.navigate("ai-settings")},identity={nav.navigate("identity")})
            }
            composable("conversation/{id}") {
                val dvm: DebateViewModel=viewModel(factory=viewModelFactory {initializer {DebateViewModel(vm.graph,createSavedStateHandle())}})
                DebateScreen(dvm,library,journal,{vm.toggleFavorite("conversation",it)},{nav.navigate("conversations")},{nav.popBackStack()},configure={nav.navigate("ai-settings")},identity={nav.navigate("identity")})
            }
            composable("conversations") {ConversationHistoryScreen(conversations,{nav.popBackStack()},{nav.navigate("conversation/$it")})}
            composable("profile") { ProfileScreen(library,journal,{nav.navigate("school/$it")},{nav.navigate("history")},{nav.navigate("favorites")},{nav.navigate("snapshots")},{nav.navigate("settings")},identity,{nav.navigate("identity")},{nav.navigate("bgm")}) }
            composable("identity") { val ivm: IdentityViewModel=viewModel(factory=viewModelFactory {initializer {IdentityViewModel(vm.graph.preferences,createSavedStateHandle())}});IdentityScreen(ivm,{nav.popBackStack()}) }
            composable("history") { HistoryScreen(library,journal,{nav.popBackStack()}) {nav.navigate("result/$it")} }
            composable("favorites") { FavoriteScreen(library,journal,{nav.popBackStack()},{nav.navigate("${it.type}/${it.targetId}")},{vm.toggleFavorite(it.type,it.targetId)},{id->conversations.find {it.id==id}?.topic ?: "讨论已被清除"}) }
            composable("snapshots") { SnapshotScreen(journal,{nav.popBackStack()}) }
            composable("settings") {
                val svm: SettingsViewModel=viewModel(factory=viewModelFactory {initializer {SettingsViewModel(vm.graph)}})
                SettingsScreen(svm,{nav.popBackStack()},{if(it=="model") nav.navigate("ai-settings") else if(it=="bgm") nav.navigate("bgm") else nav.navigate("info/$it")})
            }
            composable("bgm") {
                val bvm: BgmViewModel=viewModel(factory=viewModelFactory {initializer {BgmViewModel(vm.graph.bgm)}})
                BgmScreen(bvm,{nav.popBackStack()})
            }
            composable("ai-settings") {
                val mvm: ModelSettingsViewModel=viewModel(factory=viewModelFactory {initializer {ModelSettingsViewModel(vm.graph,createSavedStateHandle())}})
                ModelSettingsScreen(mvm,{nav.popBackStack()})
            }
            composable("info/{kind}") {e->InfoScreen(e.arguments?.getString("kind").orEmpty(),{nav.popBackStack()})}
            composable("question/{id}?choice={choice}",arguments=listOf(androidx.navigation.navArgument("choice") { nullable=true; defaultValue=null })) { e ->
                val qvm: QuestionViewModel = viewModel(factory=viewModelFactory { initializer { QuestionViewModel(vm.graph.repository,createSavedStateHandle()) } })
                QuestionScreen(library.questions.find { it.id==e.arguments?.getString("id") },qvm,e.arguments?.getString("choice"),{nav.popBackStack()}) { id -> nav.navigate("result/$id") { popUpTo(e.destination.id) { inclusive=true } } }
            }
            composable("result/{id}") { e ->
                val answer=journal.answers.find { it.id==e.arguments?.getString("id")?.toLongOrNull() }
                val rvm: ResultViewModel = viewModel(factory=viewModelFactory { initializer { ResultViewModel(vm.graph.repository,createSavedStateHandle()) } })
                ResultScreen(library.questions.find { it.id==answer?.questionId },answer,rvm,{nav.popBackStack()},{nav.navigate("school/$it")},{tab("profile")},journal,{answer?.let {FavoriteButton("question",it.questionId,journal) {vm.toggleFavorite("question",it.questionId)}}},{nav.navigate("question/$it")})
            }
            composable("school/{id}") { e -> val id=e.arguments?.getString("id").orEmpty();SchoolDetailScreen(library.schools.find {it.id==id},library,{nav.popBackStack()},{nav.navigate("philosopher/$it")},{nav.navigate("question/$it")},{FavoriteButton("school",id,journal) {vm.toggleFavorite("school",id)}}) }
            composable("philosopher/{id}") { e -> val id=e.arguments?.getString("id").orEmpty();PhilosopherDetailScreen(library.philosophers.find {it.id==id},library,{nav.popBackStack()},{nav.navigate("school/$it")},{nav.navigate("question/$it")},{nav.navigate("map")},{FavoriteButton("philosopher",id,journal) {vm.toggleFavorite("philosopher",id)}}) }
            composable("map") { PhilosophyMapScreen(library,{nav.popBackStack()},{nav.navigate("philosopher/$it")}) }
        }
    }
}


